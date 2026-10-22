package com.braintreepayments.api.paypalsavedpaymentmethod.imageloader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.braintreepayments.api.paypalsavedpaymentmethod.asHttpsUrlOrNull
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
import kotlin.coroutines.cancellation.CancellationException

/**
 * Loads funding-instrument icons into memory, de-duplicating concurrent requests for the same URL and bounding how
 * many bitmaps are fetched and decoded at once. Bytes are fetched through the SDK's pinned [HttpClient]. Dependencies
 * are injectable for tests; production code uses [getInstance].
 *
 * Decodes raster formats only (PNG, JPEG, etc.) via [android.graphics.BitmapFactory]. There is currently no image
 * loader in Braintree Native, and this custom implementation does not support SVG, matching standard Android image
 * loading in general (BitmapFactory, and Coil/Glide without an SVG add-on).
 *
 * Call [clearCache] from the host app's memory-pressure callback to release cached bitmaps; see [clearCache] for why
 * this loader can't do that on its own.
 */
internal class ImageLoader internal constructor(
    private val httpClient: HttpClient = HttpClient(),
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main.immediate,
    private val memoryCache: LruCache<String, Bitmap> = createMemoryCache(),
    private val decodeSemaphore: Semaphore = Semaphore(DEFAULT_MAX_PARALLEL_DECODES),
    private val fetchSemaphore: Semaphore = Semaphore(DEFAULT_MAX_PARALLEL_FETCHES),
) {
    // Deliberately unscoped to any lifecycle: ImageLoader is a process-wide singleton (see getInstance()), so
    // in-flight downloads outlive any single screen and are only ever torn down by process death. That is safe because
    // each download is short and bounded (30 s call timeout, size cap) and failures aren't cached, so nothing
    // accumulates.
    private val loaderScope = CoroutineScope(SupervisorJob() + defaultDispatcher)
    private val inFlightRequests = ConcurrentHashMap<String, Deferred<Bitmap>>()

    // Weak keys: the loader is a process-wide singleton and must never keep a view alive.
    private val viewRequests = Collections.synchronizedMap(WeakHashMap<ImageView, ViewRequest>())

    private val attachStateListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            val imageView = v as? ImageView ?: return
            // Resume a load whose wait was cancelled by a detach, e.g. a RecyclerView row re-attached without re-bind.
            val request = viewRequests[imageView]
            if (request != null && request.job.isCancelled) {
                startViewRequest(imageView, request.url, request.errorPlaceholder)
            }
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
     * Evicts every cached bitmap. The loader holds no [android.content.Context] and so cannot register for memory
     * callbacks itself; call this from the host app's [android.content.ComponentCallbacks2.onTrimMemory] (e.g.
     * `Application.onTrimMemory`) to release the cache under memory pressure. Safe to call at any time -- in-flight
     * downloads are unaffected and simply re-populate the cache on completion.
     */
    fun clearCache() {
        memoryCache.evictAll()
    }

    /**
     * Returns the bitmap for [url], from memory when cached. Throws [IllegalArgumentException] if [url] is not an https
     * URL with a host, and [IOException] if the download or decode fails.
     */
    suspend fun load(url: String): Bitmap {
        val validUrl = requireNotNull(url.asHttpsUrlOrNull()) { "url must be an https URL with a host" }
        memoryCache.get(validUrl)?.let { return it }
        return inFlightRequest(validUrl).await()
    }

    /**
     * Binds [url] into [imageView]. While the image loads, the view shows [loadingPlaceholder], or stays blank when it
     * is `null`. If [url] is missing or not an https URL with a host, or the load fails, it shows [errorPlaceholder],
     * or stays blank when it is `null`.
     *
     * Must be called on the main thread; it updates [imageView] directly.
     */
    fun load(
        url: String?,
        imageView: ImageView,
        @DrawableRes loadingPlaceholder: Int? = null,
        @DrawableRes errorPlaceholder: Int? = null,
    ) {
        cancel(imageView)
        val validUrl = url.asHttpsUrlOrNull()
        val cached = validUrl?.let { memoryCache.get(it) }
        if (cached != null) {
            imageView.setImageBitmap(cached)
            return
        }
        if (validUrl == null) {
            imageView.setPlaceholder(errorPlaceholder)
            return
        }
        imageView.setPlaceholder(loadingPlaceholder)

        // Remove-then-add keeps exactly one listener per view, however many times the view is re-bound.
        imageView.removeOnAttachStateChangeListener(attachStateListener)
        imageView.addOnAttachStateChangeListener(attachStateListener)
        startViewRequest(imageView, validUrl, errorPlaceholder)
    }

    /** Cancels any pending load for [imageView]. Must be called on the main thread. */
    fun cancel(imageView: ImageView) {
        // Symmetric with the add in load(): once a request is fully cancelled there is nothing left to resume on
        // a future re-attach, so the view shouldn't keep carrying the listener.
        imageView.removeOnAttachStateChangeListener(attachStateListener)
        viewRequests.remove(imageView)?.job?.cancel()
    }

    private fun inFlightRequest(url: String): Deferred<Bitmap> =
        inFlightRequests[url] ?: synchronized(inFlightRequests) {
            inFlightRequests[url] ?: loaderScope.async(start = CoroutineStart.LAZY) {
                // Another download may have cached this URL after the caller's cache miss and before this started.
                memoryCache.get(url) ?: fetchAndDecode(url)
            }.also { deferred ->
                inFlightRequests[url] = deferred
                // Registered after the put and before start, so a download that finishes immediately cannot leave
                // a stale (possibly failed) entry behind. Waiter cancellation never removes the entry.
                deferred.invokeOnCompletion { inFlightRequests.remove(url, deferred) }
                deferred.start()
            }
        }

    private fun startViewRequest(imageView: ImageView, url: String, @DrawableRes errorPlaceholder: Int?) {
        val target = WeakReference(imageView)
        val job = loaderScope.launch(mainDispatcher, start = CoroutineStart.LAZY) {
            val bitmap = try {
                load(url)
            } catch (ignored: IOException) {
                null // keep the placeholder; no automatic retry
            } catch (ignored: IllegalArgumentException) {
                null // non-https or host-less URL rejected before any request
            } catch (ignored: Exception) {
                if (ignored is CancellationException) throw ignored
                null // unexpected failure (e.g. a malformed response the decoder can't handle); keep the placeholder
            }
            val view = target.get() ?: return@launch
            // Drop the result if the view has been re-bound to a different request since this one started.
            if (viewRequests[view]?.job !== coroutineContext.job) return@launch
            viewRequests.remove(view)
            if (bitmap != null) view.setImageBitmap(bitmap) else view.setPlaceholder(errorPlaceholder)
        }
        // Recorded before start so the identity check above always sees this request.
        viewRequests[imageView] = ViewRequest(url, errorPlaceholder, job)
        job.start()
    }

    private suspend fun fetchAndDecode(url: String): Bitmap {
        // Bounds how many icon downloads run over the network at once; distinct URLs are not deduplicated like
        // repeat requests for the same URL are (see inFlightRequest), so without this a burst of distinct icons
        // (e.g. a list screen) could open one connection per icon at the same time.
        val bytes = fetchSemaphore.withPermit {
            httpClient.sendRequestForBytes(OkHttpRequest(url, Method.Get), MAX_IMAGE_BYTES)
        }
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

    // Also clears a stale image (e.g. from a recycled RecyclerView row) when there is no placeholder.
    private fun ImageView.setPlaceholder(@DrawableRes placeholder: Int?) {
        if (placeholder != null) setImageResource(placeholder) else setImageDrawable(null)
    }

    private class ViewRequest(val url: String, @DrawableRes val errorPlaceholder: Int?, val job: Job)

    companion object {
        // 4 MB keeps a reasonable number of small FI icons resident without pressuring low-memory devices.
        private const val DEFAULT_CACHE_SIZE_BYTES = 4 * 1024 * 1024

        // Bounds concurrent BitmapFactory decodes so many icons loading at once can't spike memory/CPU.
        private const val DEFAULT_MAX_PARALLEL_DECODES = 4

        // Bounds concurrent icon downloads so a burst of distinct URLs can't open unbounded connections at once.
        private const val DEFAULT_MAX_PARALLEL_FETCHES = 4

        // FI icons never render larger than this in any current layout; decoding beyond it wastes memory.
        private const val MAX_DECODE_DIMENSION_PX = 128

        // Generous ceiling for a small icon; guards against an unexpectedly large or malicious response.
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
