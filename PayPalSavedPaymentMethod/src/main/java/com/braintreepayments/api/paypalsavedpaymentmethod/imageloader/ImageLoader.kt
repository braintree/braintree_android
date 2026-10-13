package com.braintreepayments.api.paypalsavedpaymentmethod.imageloader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.braintreepayments.api.sharedutils.HttpClient
import com.braintreepayments.api.sharedutils.Method
import com.braintreepayments.api.sharedutils.OkHttpRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.Collections
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * Loads funding-instrument icons into memory, de-duplicating concurrent requests for the same URL and bounding how
 * many bitmaps are decoded at once. Bytes are fetched through the SDK's pinned [HttpClient]. Dependencies are
 * injectable for tests; production code uses [getInstance].
 */
internal class ImageLoader internal constructor(
    private val httpClient: HttpClient = HttpClient(),
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val memoryCache: LruCache<String, Bitmap> = createMemoryCache(),
    private val decodeSemaphore: Semaphore = Semaphore(DEFAULT_MAX_PARALLEL_DECODES),
) {
    // Deliberately unscoped to any lifecycle: ImageLoader is a process-wide singleton (see getInstance()), so
    // in-flight downloads outlive any single screen and are only ever torn down by process death.
    private val loaderScope = CoroutineScope(SupervisorJob() + defaultDispatcher)
    private val inFlightRequests = ConcurrentHashMap<String, Deferred<Bitmap>>()

    // Weak keys: the loader is a process-wide singleton and must never keep a view alive.
    private val viewRequests = Collections.synchronizedMap(WeakHashMap<ImageView, ViewRequest>())

    private val attachStateListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            val imageView = v as? ImageView ?: return
            // Resume a load whose wait was cancelled by a detach, e.g. a RecyclerView row re-attached without re-bind.
            val request = viewRequests[imageView]
            if (request != null && request.job.isCancelled) startViewRequest(imageView, request.url)
        }

        override fun onViewDetachedFromWindow(v: View) {
            val imageView = v as? ImageView ?: return
            // Keep the record so a re-attach can resume; only the wait is cancelled, the download keeps running.
            viewRequests[imageView]?.job?.cancel()
        }
    }

    /** Returns the cached bitmap for [url] without touching the network, or `null` on a cache miss. */
    fun peek(url: String): Bitmap? = memoryCache.get(url)

    /**
     * Returns the bitmap for [url], from memory when cached. Throws [IllegalArgumentException] if [url] is blank or
     * malformed, and [IOException] if the download or decode fails.
     */
    suspend fun load(url: String): Bitmap {
        // TODO: validate with asHttpsUrlOrNull() from UrlValidation.kt (PR #1718) once it is on this branch, so only
        //  https URLs with a host reach the network.
        require(url.isNotBlank()) { "url must not be blank" }
        memoryCache.get(url)?.let { return it }
        return inFlightRequest(url).await()
    }

    /** Binds [url] into [imageView], showing [placeholder] until the image is ready, or permanently if it fails. */
    fun load(
        url: String?,
        imageView: ImageView,
        @DrawableRes placeholder: Int?,
    ) {
        cancel(imageView)
        // TODO: validate with asHttpsUrlOrNull() from UrlValidation.kt (PR #1718) instead of a blank check.
        val validUrl = url?.takeIf { it.isNotBlank() }
        val cached = validUrl?.let { memoryCache.get(it) }
        if (cached != null) {
            imageView.setImageBitmap(cached)
            return
        }
        placeholder?.let { imageView.setImageResource(it) }
        if (validUrl == null) return

        // Remove-then-add keeps exactly one listener per view, however many times the view is re-bound.
        imageView.removeOnAttachStateChangeListener(attachStateListener)
        imageView.addOnAttachStateChangeListener(attachStateListener)
        startViewRequest(imageView, validUrl)
    }

    fun cancel(imageView: ImageView) {
        // Symmetric with the add in load(): once a request is fully cancelled there is nothing left to resume on
        // a future re-attach, so the view shouldn't keep carrying the listener.
        imageView.removeOnAttachStateChangeListener(attachStateListener)
        viewRequests.remove(imageView)?.job?.cancel()
    }

    private fun inFlightRequest(url: String): Deferred<Bitmap> =
        inFlightRequests[url] ?: synchronized(inFlightRequests) {
            inFlightRequests[url] ?: loaderScope.async(start = CoroutineStart.LAZY) {
                fetchAndDecode(url)
            }.also { deferred ->
                inFlightRequests[url] = deferred
                // Registered after the put and before start, so a download that finishes immediately cannot leave
                // a stale (possibly failed) entry behind. Waiter cancellation never removes the entry.
                deferred.invokeOnCompletion { inFlightRequests.remove(url, deferred) }
                deferred.start()
            }
        }

    private fun startViewRequest(imageView: ImageView, url: String) {
        val target = WeakReference(imageView)
        val job = loaderScope.launch(mainDispatcher, start = CoroutineStart.LAZY) {
            val bitmap = try {
                load(url)
            } catch (ignored: IOException) {
                null // keep the placeholder; no automatic retry
            } catch (ignored: IllegalArgumentException) {
                null // malformed URL rejected before any request; see the URL validation TODO in load()
            }
            val view = target.get() ?: return@launch
            // Drop the result if the view has been re-bound to a different request since this one started.
            if (viewRequests[view]?.job !== coroutineContext.job) return@launch
            viewRequests.remove(view)
            bitmap?.let(view::setImageBitmap)
        }
        // Recorded before start so the identity check above always sees this request.
        viewRequests[imageView] = ViewRequest(url, job)
        job.start()
    }

    private suspend fun fetchAndDecode(url: String): Bitmap {
        val bytes = httpClient.sendRequestForBytes(OkHttpRequest(url, Method.Get), MAX_IMAGE_BYTES)
        val bitmap = withContext(defaultDispatcher) {
            decodeSemaphore.withPermit { decodeBitmap(bytes, url) }
        }
        memoryCache.put(url, bitmap)
        return bitmap
    }

    // Two-pass decode: read bounds only, then decode a power-of-two subsample close to the display size.
    // Reference: https://developer.android.com/topic/performance/graphics/load-bitmap
    private fun decodeBitmap(bytes: ByteArray, url: String): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds, MAX_DECODE_DIMENSION_PX, MAX_DECODE_DIMENSION_PX)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: throw IOException("Unable to decode image from $url")
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int,
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        while (height / inSampleSize > reqHeight || width / inSampleSize > reqWidth) {
            inSampleSize *= 2
        }

        return inSampleSize
    }

    private class ViewRequest(val url: String, val job: Job)

    companion object {
        private const val DEFAULT_CACHE_SIZE_BYTES = 4 * 1024 * 1024
        private const val DEFAULT_MAX_PARALLEL_DECODES = 4
        private const val MAX_DECODE_DIMENSION_PX = 128
        private const val MAX_IMAGE_BYTES = 1024 * 1024L

        private fun createMemoryCache() = object : LruCache<String, Bitmap>(DEFAULT_CACHE_SIZE_BYTES) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
        }

        @Volatile
        private var instance: ImageLoader? = null

        fun getInstance(): ImageLoader =
            instance ?: synchronized(this) {
                instance ?: ImageLoader().also { instance = it }
            }
    }
}
