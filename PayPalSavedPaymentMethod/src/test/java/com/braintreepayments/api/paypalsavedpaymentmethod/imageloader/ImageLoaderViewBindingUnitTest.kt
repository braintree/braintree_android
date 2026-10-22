package com.braintreepayments.api.paypalsavedpaymentmethod.imageloader

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.util.LruCache
import android.widget.FrameLayout
import android.widget.ImageView
import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.sharedutils.HttpClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ImageLoaderViewBindingUnitTest {

    private val testDispatcher = StandardTestDispatcher()
    private val httpClient = mockk<HttpClient>()
    private val sut = ImageLoader(
        httpClient = httpClient,
        defaultDispatcher = testDispatcher,
        mainDispatcher = testDispatcher,
        memoryCache = LruCache<String, Bitmap>(4 * 1024 * 1024),
        decodeSemaphore = Semaphore(4),
    )
    private val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    private val imageView = ImageView(activity).also { activity.setContentView(it) }

    @Test
    fun `load stays blank while loading and then shows the downloaded bitmap`() {
        givenImage(32)

        sut.load(URL, imageView, errorPlaceholder = ERROR_PLACEHOLDER)
        assertBlank()

        advanceUntilIdle()
        assertShowsBitmapOfWidth(32)
    }

    @Test
    fun `load shows the loading placeholder while loading`() {
        givenImage(32)

        sut.load(URL, imageView, loadingPlaceholder = LOADING_PLACEHOLDER)
        assertShowsPlaceholder()

        advanceUntilIdle()
        assertShowsBitmapOfWidth(32)
    }

    @Test
    fun `load sets a cached bitmap immediately`() = runTest(testDispatcher) {
        givenImage(32)
        sut.load(URL)

        sut.load(URL, imageView, LOADING_PLACEHOLDER, ERROR_PLACEHOLDER)

        assertShowsBitmapOfWidth(32)
    }

    @Test
    fun `load shows the error placeholder for a null, blank or non-https url without making a request`() {
        listOf(null, "  ", "http://www.paypalobjects.com/images/visa.png").forEach { url ->
            sut.load(url, imageView, LOADING_PLACEHOLDER, ERROR_PLACEHOLDER)
            advanceUntilIdle()
            assertShowsPlaceholder()
        }
        coVerify(exactly = 0) { httpClient.sendRequestForBytes(any(), any()) }
    }

    @Test
    fun `load shows the error placeholder when the download fails or the url is rejected as malformed`() {
        listOf(IOException("Unexpected code 404"), IllegalArgumentException("Invalid URL host")).forEach { error ->
            coEvery { httpClient.sendRequestForBytes(any(), any()) } throws error

            sut.load(URL, imageView, errorPlaceholder = ERROR_PLACEHOLDER)
            assertBlank()
            advanceUntilIdle()

            assertShowsPlaceholder()
        }
    }

    @Test
    fun `load goes blank when the download fails and there is no error placeholder`() {
        coEvery { httpClient.sendRequestForBytes(any(), any()) } throws IOException("Unexpected code 404")

        sut.load(URL, imageView, loadingPlaceholder = LOADING_PLACEHOLDER)
        assertShowsPlaceholder()
        advanceUntilIdle()

        assertBlank()
    }

    @Test
    fun `re-binding the view before the first load completes shows only the latest image`() {
        coEvery { httpClient.sendRequestForBytes(match { it.url == URL }, any()) } returns pngBytes(16, 16)
        coEvery { httpClient.sendRequestForBytes(match { it.url == OTHER_URL }, any()) } returns pngBytes(48, 48)

        sut.load(URL, imageView, errorPlaceholder = ERROR_PLACEHOLDER)
        sut.load(OTHER_URL, imageView, errorPlaceholder = ERROR_PLACEHOLDER)
        advanceUntilIdle()

        assertShowsBitmapOfWidth(48)
    }

    @Test
    fun `a detached view resumes its load when re-attached`() {
        givenImage(32)

        sut.load(URL, imageView, errorPlaceholder = ERROR_PLACEHOLDER)
        activity.setContentView(FrameLayout(activity))
        advanceUntilIdle()
        assertBlank()

        activity.setContentView(imageView)
        advanceUntilIdle()
        assertShowsBitmapOfWidth(32)
    }

    @Test
    fun `cancel stops the bitmap from being set`() {
        givenImage(32)

        sut.load(URL, imageView, errorPlaceholder = ERROR_PLACEHOLDER)
        sut.cancel(imageView)
        advanceUntilIdle()

        assertBlank()
    }

    private fun givenImage(size: Int) {
        coEvery { httpClient.sendRequestForBytes(any(), any()) } returns pngBytes(size, size)
    }

    private fun advanceUntilIdle() = testDispatcher.scheduler.advanceUntilIdle()

    private fun assertBlank() {
        assertNull(imageView.drawable)
    }

    private fun assertShowsPlaceholder() {
        assertNotNull(imageView.drawable)
        assertFalse(imageView.drawable is BitmapDrawable)
    }

    private fun assertShowsBitmapOfWidth(width: Int) {
        assertEquals(width, (imageView.drawable as BitmapDrawable).bitmap.width)
    }

    companion object {
        private const val URL = "https://www.paypalobjects.com/images/visa.png"
        private const val OTHER_URL = "https://www.paypalobjects.com/images/bank.png"
        private val LOADING_PLACEHOLDER = R.drawable.ic_fi_bank_placeholder
        private val ERROR_PLACEHOLDER = R.drawable.ic_fi_card_placeholder
    }
}
