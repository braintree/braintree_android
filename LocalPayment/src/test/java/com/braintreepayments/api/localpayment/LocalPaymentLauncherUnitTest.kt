package com.braintreepayments.api.localpayment

import android.content.Intent
import androidx.activity.ComponentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchStartResult
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocalPaymentLauncherUnitTest {
    private val browserSwitchClient: BrowserSwitchClient = mockk(relaxed = true)
    private val activity: ComponentActivity = mockk(relaxed = true)
    private val intent: Intent = mockk(relaxed = true)
    private val pendingRequestString = "pending_request_string"
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
        val browserSwitchPendingRequest: BrowserSwitchStartResult =
            BrowserSwitchStartResult.Started(pendingRequestString)
        every {
            browserSwitchClient.start(eq(activity), eq(options))
        } returns browserSwitchPendingRequest
        val pendingRequest = sut.launch(
            activity,
            LocalPaymentAuthRequest.ReadyToLaunch(localPaymentAuthRequestParams)
        )
        assertTrue(pendingRequest is LocalPaymentPendingRequest.Started)
        assertSame(
            (browserSwitchPendingRequest as BrowserSwitchStartResult.Started).pendingRequest,
            (pendingRequest as LocalPaymentPendingRequest.Started).pendingRequestString
        )
    }

    @Test
    fun `launch on error returns failure`() {
        val exception = BrowserSwitchException("error")
        every {
            browserSwitchClient.start(eq(activity), eq(options))
        } returns BrowserSwitchStartResult.Failure(exception)

        val pendingRequest = sut.launch(
            activity,
            LocalPaymentAuthRequest.ReadyToLaunch(localPaymentAuthRequestParams)
        )

        assertTrue(pendingRequest is LocalPaymentPendingRequest.Failure)
        assertEquals(exception, (pendingRequest as LocalPaymentPendingRequest.Failure).error)
    }

    @Test
    fun `handleReturnToAppFromBrowser on BrowserSwitchResult returns result`() {
        val browserSwitchFinalResult = mockk<BrowserSwitchFinalResult.Success>()
        val pendingRequest: LocalPaymentPendingRequest.Started =
            LocalPaymentPendingRequest.Started(pendingRequestString)
        every {
            browserSwitchClient.completeRequest(eq(intent), eq(pendingRequestString))
        } returns browserSwitchFinalResult

        val paymentAuthResult = sut.handleReturnToAppFromBrowser(pendingRequest, intent)

        assertTrue(paymentAuthResult is LocalPaymentAuthResult.Success)
        assertSame(
            browserSwitchFinalResult,
            (paymentAuthResult as LocalPaymentAuthResult.Success).browserSwitchSuccess
        )
    }

    @Test
    fun `handleReturnToAppFromBrowser when no BrowserSwitchResult returns null`() {
        val pendingRequest: LocalPaymentPendingRequest.Started =
            LocalPaymentPendingRequest.Started(pendingRequestString)
        every {
            browserSwitchClient.completeRequest(eq(intent), eq(pendingRequestString))
        } returns BrowserSwitchFinalResult.NoResult

        val paymentAuthResult = sut.handleReturnToAppFromBrowser(pendingRequest, intent)

        assertTrue(paymentAuthResult is LocalPaymentAuthResult.NoResult)
    }
}
