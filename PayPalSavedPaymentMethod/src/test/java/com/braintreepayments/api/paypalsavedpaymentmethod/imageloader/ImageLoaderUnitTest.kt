package com.braintreepayments.api.paypalsavedpaymentmethod.imageloader

import android.graphics.Bitmap
import android.util.LruCache
import com.braintreepayments.api.sharedutils.HttpClient
import com.braintreepayments.api.sharedutils.Method
import com.braintreepayments.api.sharedutils.OkHttpRequest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.IOException
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImageLoaderUnitTest {

    private val testDispatcher = StandardTestDispatcher()
    private val httpClient = mockk<HttpClient>()
    private var sut = createImageLoader()

    @Test
    fun `load sends a size-limited GET and returns the bitmap without upscaling`() = runTest(testDispatcher) {
        val request = slot<OkHttpRequest>()
        coEvery { httpClient.sendRequestForBytes(capture(request), 1024 * 1024L) } returns pngBytes(32, 32)

        val bitmap = sut.load(URL)

        assertEquals(32, bitmap.width)
        assertEquals(URL, request.captured.url)
        assertEquals(Method.Get, request.captured.method)
    }

    @Test
    fun `load serves a repeated url from the memory cache`() = runTest(testDispatcher) {
        givenImage(32)
        assertNull(sut.peek(URL))

        val first = sut.load(URL)

        assertSame(first, sut.peek(URL))
        assertSame(first, sut.load(URL))
        coVerify(exactly = 1) { httpClient.sendRequestForBytes(any(), any()) }
    }

    @Test
    fun `concurrent loads of the same url share one request`() = runTest(testDispatcher) {
        givenImage(32)

        val results = listOf(async { sut.load(URL) }, async { sut.load(URL) }).awaitAll()

        assertSame(results[0], results[1])
        coVerify(exactly = 1) { httpClient.sendRequestForBytes(any(), any()) }
    }

    @Test
    fun `cancelling a waiter does not start a second request for the same url`() = runTest(testDispatcher) {
        val gate = CompletableDeferred<Unit>()
        coEvery { httpClient.sendRequestForBytes(any(), any()) } coAnswers {
            gate.await()
            pngBytes(32, 32)
        }

        val firstWaiter = launch { sut.load(URL) }
        runCurrent()
        firstWaiter.cancel()
        val secondWaiter = async { sut.load(URL) }
        runCurrent()
        gate.complete(Unit)

        assertEquals(32, secondWaiter.await().width)
        coVerify(exactly = 1) { httpClient.sendRequestForBytes(any(), any()) }
    }

    @Test
    fun `a failed load is not cached and the next load makes a new request`() = runTest(testDispatcher) {
        var calls = 0
        coEvery { httpClient.sendRequestForBytes(any(), any()) } answers {
            if (++calls == 1) throw IOException("Unexpected code 500") else pngBytes(32, 32)
        }

        assertFailsWith<IOException> { sut.load(URL) }

        assertEquals(32, sut.load(URL).width)
        coVerify(exactly = 2) { httpClient.sendRequestForBytes(any(), any()) }
    }

    @Test
    fun `load throws IOException and caches nothing when the bytes cannot be decoded`() = runTest(testDispatcher) {
        coEvery { httpClient.sendRequestForBytes(any(), any()) } returns byteArrayOf(1, 2, 3)

        assertFailsWith<IOException> { sut.load(URL) }
        assertNull(sut.peek(URL))
    }

    @Test
    fun `load throws IllegalArgumentException for a blank or non-https url without making a request`() =
        runTest(testDispatcher) {
            listOf("", "   ", "http://www.paypalobjects.com/images/visa.png", "javascript:alert(1)", "https://")
                .forEach { url -> assertFailsWith<IllegalArgumentException> { sut.load(url) } }

            coVerify(exactly = 0) { httpClient.sendRequestForBytes(any(), any()) }
        }

    @Test
    fun `load downsamples a large image to at most 128 px`() = runTest(testDispatcher) {
        givenImage(1024)

        val bitmap = sut.load(URL)

        assertEquals(128, bitmap.width)
        assertEquals(128, bitmap.height)
    }

    @Test
    fun `decoding waits for a decode permit`() = runTest(testDispatcher) {
        val decodeSemaphore = Semaphore(1).apply { acquire() }
        sut = createImageLoader(decodeSemaphore)
        givenImage(32)

        val result = async { sut.load(URL) }
        advanceUntilIdle()
        assertFalse(result.isCompleted)

        decodeSemaphore.release()
        assertEquals(32, result.await().width)
    }

    @Test
    fun `fetching waits for a fetch permit`() = runTest(testDispatcher) {
        val fetchSemaphore = Semaphore(1).apply { acquire() }
        sut = createImageLoader(fetchSemaphore = fetchSemaphore)
        givenImage(32)

        val result = async { sut.load(URL) }
        advanceUntilIdle()
        assertFalse(result.isCompleted)
        coVerify(exactly = 0) { httpClient.sendRequestForBytes(any(), any()) }

        fetchSemaphore.release()
        assertEquals(32, result.await().width)
    }

    @Test
    fun `clearCache evicts cached bitmaps`() = runTest(testDispatcher) {
        givenImage(32)
        sut.load(URL)
        assertEquals(32, sut.peek(URL)?.width)

        sut.clearCache()

        assertNull(sut.peek(URL))
    }

    @Test
    fun `getInstance always returns the same loader`() {
        assertSame(ImageLoader.getInstance(), ImageLoader.getInstance())
    }

    private fun givenImage(size: Int) {
        coEvery { httpClient.sendRequestForBytes(any(), any()) } returns pngBytes(size, size)
    }

    private fun createImageLoader(
        decodeSemaphore: Semaphore = Semaphore(4),
        fetchSemaphore: Semaphore = Semaphore(4),
    ) = ImageLoader(
        httpClient = httpClient,
        defaultDispatcher = testDispatcher,
        mainDispatcher = testDispatcher,
        memoryCache = LruCache<String, Bitmap>(4 * 1024 * 1024),
        decodeSemaphore = decodeSemaphore,
        fetchSemaphore = fetchSemaphore,
    )

    companion object {
        private const val URL = "https://www.paypalobjects.com/images/visa.png"
    }
}
