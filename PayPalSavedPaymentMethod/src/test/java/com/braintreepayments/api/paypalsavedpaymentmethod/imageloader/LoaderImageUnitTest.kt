package com.braintreepayments.api.paypalsavedpaymentmethod.imageloader

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.braintreepayments.api.sharedutils.HttpClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LoaderImageUnitTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private val httpClient = mockk<HttpClient>()
    private val sut = ImageLoader(
        httpClient = httpClient,
        defaultDispatcher = testDispatcher,
        mainDispatcher = testDispatcher,
        memoryCache = LruCache<String, Bitmap>(4 * 1024 * 1024),
        decodeSemaphore = Semaphore(4),
    )
    private val placeholder = ColorPainter(Color.Red)

    @Test
    fun `shows the placeholder then the loaded image`() {
        coEvery { httpClient.sendRequestForBytes(any(), any()) } returns pngBytes(32, 32)

        composeTestRule.setContent {
            LoaderImage(url = URL, placeholder = placeholder, imageLoader = sut)
        }

        composeTestRule.onNodeWithTag(PLACEHOLDER_IMAGE_TEST_TAG).assertExists()
        composeTestRule.onNodeWithTag(LOADED_IMAGE_TEST_TAG).assertDoesNotExist()

        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(LOADED_IMAGE_TEST_TAG).assertExists()
        composeTestRule.onNodeWithTag(PLACEHOLDER_IMAGE_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `shows the cached image on the first frame without flashing the placeholder`() = runTest(testDispatcher) {
        coEvery { httpClient.sendRequestForBytes(any(), any()) } returns pngBytes(32, 32)
        sut.load(URL)

        composeTestRule.setContent {
            LoaderImage(url = URL, placeholder = placeholder, imageLoader = sut)
        }

        composeTestRule.onNodeWithTag(LOADED_IMAGE_TEST_TAG).assertExists()
        composeTestRule.onNodeWithTag(PLACEHOLDER_IMAGE_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `keeps the placeholder when the load fails`() {
        coEvery { httpClient.sendRequestForBytes(any(), any()) } throws IOException("boom")

        composeTestRule.setContent {
            LoaderImage(url = URL, placeholder = placeholder, imageLoader = sut)
        }
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(PLACEHOLDER_IMAGE_TEST_TAG).assertExists()
        composeTestRule.onNodeWithTag(LOADED_IMAGE_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun `loads the new image after the url changes`() {
        coEvery { httpClient.sendRequestForBytes(match { it.url == URL }, any()) } returns pngBytes(16, 16)
        coEvery { httpClient.sendRequestForBytes(match { it.url == OTHER_URL }, any()) } returns pngBytes(48, 48)

        var currentUrl by mutableStateOf(URL)
        composeTestRule.setContent {
            LoaderImage(url = currentUrl, placeholder = placeholder, imageLoader = sut)
        }
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(LOADED_IMAGE_TEST_TAG).assertExists()

        currentUrl = OTHER_URL
        // Recomposition from an external state write only happens once Compose processes a frame; only then does
        // the new LaunchedEffect start and queue work on testDispatcher for advanceUntilIdle() to drain.
        composeTestRule.waitForIdle()
        testDispatcher.scheduler.advanceUntilIdle()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(LOADED_IMAGE_TEST_TAG).assertExists()
        coVerify(exactly = 1) { httpClient.sendRequestForBytes(match { it.url == OTHER_URL }, any()) }
    }

    private companion object {
        const val URL = "https://www.paypalobjects.com/images/visa.png"
        const val OTHER_URL = "https://www.paypalobjects.com/images/bank.png"
    }
}
