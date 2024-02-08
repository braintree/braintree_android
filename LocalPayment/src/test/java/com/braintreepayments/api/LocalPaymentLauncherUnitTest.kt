package com.braintreepayments.api

import android.content.Intent
import androidx.activity.ComponentActivity
import io.mockk.every
import io.mockk.mockk
import net.bytebuddy.asm.Advice.Local
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalPaymentLauncherUnitTest {
    private val browserSwitchClient: BrowserSwitchClient = mockk(relaxed = true)
    private val activity: ComponentActivity = mockk(relaxed = true)
    private val intent: Intent = mockk(relaxed = true)
    private val browserSwitchRequest: BrowserSwitchRequest = mockk(relaxed = true)
    private val options: BrowserSwitchOptions = mockk(relaxed = true)
    private val localPaymentAuthRequestParams: LocalPaymentAuthRequestParams = mockk(relaxed = true)
    private lateinit var sut: LocalPaymentLauncher

    @Before
    fun beforeEach() {
        every { localPaymentAuthRequestParams.browserSwitchOptions } returns options
        sut = LocalPaymentLauncher(browserSwitchClient)
    }

    @Test
    fun `launch starts browser switch and returns pending request`() {
        val browserSwitchPendingRequest: BrowserSwitchPendingRequest =
            BrowserSwitchPendingRequest.Started(browserSwitchRequest)
        every {
            browserSwitchClient.start(eq(activity), eq(options))
        } returns browserSwitchPendingRequest
        val pendingRequest = sut.launch(
            activity,
            LocalPaymentAuthRequest.ReadyToLaunch(localPaymentAuthRequestParams)
        )
        assertTrue(pendingRequest is LocalPaymentPendingRequest.Started)
        assertSame(
            browserSwitchPendingRequest,
            (pendingRequest as LocalPaymentPendingRequest.Started).request
        )
    }

    @Test
    fun `launch on error returns failure`() {
        val exception = BrowserSwitchException("error")
        every {
            browserSwitchClient.start(eq(activity), eq(options))
        } returns BrowserSwitchPendingRequest.Failure(exception)

        val pendingRequest = sut.launch(
            activity,
            LocalPaymentAuthRequest.ReadyToLaunch(localPaymentAuthRequestParams)
        )

        assertTrue(pendingRequest is LocalPaymentPendingRequest.Failure)
        assertEquals(exception, (pendingRequest as LocalPaymentPendingRequest.Failure).error)
    }

    @Test
    fun `handleReturnToAppFromBrowser on BrowserSwitchResult returns result`() {
        val browserSwitchResultInfo = Mockito.mock(
            BrowserSwitchResultInfo::class.java
        )
        val browserSwitchPendingRequest = BrowserSwitchPendingRequest.Started(browserSwitchRequest)
        val pendingRequest: LocalPaymentPendingRequest.Started =
            LocalPaymentPendingRequest.Started(browserSwitchPendingRequest)
        every {
            browserSwitchClient.parseResult(eq(browserSwitchPendingRequest), eq(intent))
        } returns BrowserSwitchResult.Success(browserSwitchResultInfo)

        val paymentAuthResult = sut.handleReturnToAppFromBrowser(pendingRequest, intent)

        assertTrue(paymentAuthResult is LocalPaymentAuthResult.Success)
        assertSame((paymentAuthResult as LocalPaymentAuthResult.Success).paymentAuthInfo.browserSwitchResult, browserSwitchResultInfo)
    }

    @Test
    fun `handleReturnToAppFromBrowser when no BrowserSwitchResult returns null`() {
        val browserSwitchPendingRequest = BrowserSwitchPendingRequest.Started(browserSwitchRequest)
        val pendingRequest: LocalPaymentPendingRequest.Started =
            LocalPaymentPendingRequest.Started(browserSwitchPendingRequest)
        every {
            browserSwitchClient.parseResult(eq(browserSwitchPendingRequest), eq(intent))
        } returns BrowserSwitchResult.NoResult

        val paymentAuthResult = sut.handleReturnToAppFromBrowser(pendingRequest, intent)

        assertTrue(paymentAuthResult is LocalPaymentAuthResult.NoResult)
    }
}
