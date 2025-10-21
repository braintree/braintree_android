package com.braintreepayments.api.sepadirectdebit

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchStartResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SEPADirectDebitLauncherUnitTest {
    private var browserSwitchClient: BrowserSwitchClient = mockk(relaxed = true)
    private val pendingRequestString = "pending_request_string"
    private var activity: ComponentActivity = mockk(relaxed = true)
    private var intent: Intent = mockk(relaxed = true)
    private var options: BrowserSwitchOptions = mockk(relaxed = true)
    private val sepaResponse: SEPADirectDebitPaymentAuthRequestParams = mockk(relaxed = true)
    private lateinit var sut: SEPADirectDebitLauncher

    @Before
    fun beforeEach() {
        sut = SEPADirectDebitLauncher(browserSwitchClient)
        every { sepaResponse.browserSwitchOptions } returns options
    }

    @Test
    fun `launch on success starts browser switch returns pending request`() {
        val browserSwitchPendingRequest: BrowserSwitchStartResult =
            BrowserSwitchStartResult.Started(pendingRequestString)
        every { browserSwitchClient.start(activity, options) } returns browserSwitchPendingRequest

        val pendingRequest = sut.launch(
            activity,
            SEPADirectDebitPaymentAuthRequest.ReadyToLaunch(sepaResponse)
        )

        assertTrue(pendingRequest is SEPADirectDebitPendingRequest.Started)
        assertSame(
            (browserSwitchPendingRequest as BrowserSwitchStartResult.Started).pendingRequest,
            (pendingRequest as SEPADirectDebitPendingRequest.Started).pendingRequestString
        )
    }

    @Test
    fun `launch on error returns failure`() {
        val exception = BrowserSwitchException("error")
        val browserSwitchPendingRequest: BrowserSwitchStartResult =
            BrowserSwitchStartResult.Failure(exception)
        every { browserSwitchClient.start(activity, options) } returns browserSwitchPendingRequest

        val pendingRequest = sut.launch(
            activity,
            SEPADirectDebitPaymentAuthRequest.ReadyToLaunch(sepaResponse)
        )

        assertTrue(pendingRequest is SEPADirectDebitPendingRequest.Failure)
        assertSame(
            exception,
            (pendingRequest as SEPADirectDebitPendingRequest.Failure).error
        )
    }

    @Test
    fun `handleReturnToApp on browser switch result returns success result`() {
        val browserSwitchFinalResult = mockk<BrowserSwitchFinalResult.Success>()
        val pendingRequest: SEPADirectDebitPendingRequest.Started =
            SEPADirectDebitPendingRequest.Started(pendingRequestString)
        every {
            browserSwitchClient.completeRequest(eq(intent), eq(pendingRequestString))
        } returns browserSwitchFinalResult

        val paymentAuthResult = sut.handleReturnToApp(pendingRequest, intent)

        assertTrue(paymentAuthResult is SEPADirectDebitPaymentAuthResult.Success)
        assertSame(
            (paymentAuthResult as SEPADirectDebitPaymentAuthResult.Success).browserSwitchSuccess,
            browserSwitchFinalResult
        )
    }

    @Test
    fun `handleReturnToApp when no BrowserSwitchResult returns no result`() {
        val pendingRequest: SEPADirectDebitPendingRequest.Started =
            SEPADirectDebitPendingRequest.Started(pendingRequestString)
        every {
            browserSwitchClient.completeRequest(eq(intent), eq(pendingRequestString))
        } returns BrowserSwitchFinalResult.NoResult

        val paymentAuthResult = sut.handleReturnToApp(pendingRequest, intent)

        assertTrue(paymentAuthResult is SEPADirectDebitPaymentAuthResult.NoResult)
    }

    @Test
    fun `constructor with ActivityResultCaller creates browserSwitchClient with provided caller`() {
        val activityResultCaller: ActivityResultCaller = mockk(relaxed = true)
        val mockBrowserSwitchClient: BrowserSwitchClient = mockk(relaxed = true)

        io.mockk.mockkConstructor(BrowserSwitchClient::class)
        every { constructedWith<BrowserSwitchClient>(eq(activityResultCaller)) } returns mockBrowserSwitchClient

        val launcher = SEPADirectDebitLauncher(activityResultCaller)

        verify(exactly = 1) { constructedWith<BrowserSwitchClient>(eq(activityResultCaller)) }
    }
}
