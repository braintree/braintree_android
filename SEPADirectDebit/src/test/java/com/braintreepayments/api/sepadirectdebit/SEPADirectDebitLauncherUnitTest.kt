package com.braintreepayments.api.sepadirectdebit

import android.content.Intent
import androidx.activity.ComponentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchPendingRequest
import com.braintreepayments.api.BrowserSwitchRequest
import com.braintreepayments.api.BrowserSwitchResult
import com.braintreepayments.api.BrowserSwitchResultInfo
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SEPADirectDebitLauncherUnitTest {
    private var browserSwitchClient: BrowserSwitchClient = mockk(relaxed = true)
    private var browserSwitchRequest: BrowserSwitchRequest = mockk(relaxed = true)
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
        val browserSwitchPendingRequest: BrowserSwitchPendingRequest =
            BrowserSwitchPendingRequest.Started(browserSwitchRequest)
        every { browserSwitchClient.start(activity, options) } returns browserSwitchPendingRequest

        val pendingRequest = sut.launch(
            activity,
            SEPADirectDebitPaymentAuthRequest.ReadyToLaunch(sepaResponse)
        )

        assertTrue(pendingRequest is SEPADirectDebitPendingRequest.Started)
        assertSame(
            browserSwitchPendingRequest,
            (pendingRequest as SEPADirectDebitPendingRequest.Started).request
        )
    }

    @Test
    fun `launch on error returns failure`() {
        val exception = BrowserSwitchException("error")
        val browserSwitchPendingRequest: BrowserSwitchPendingRequest =
            BrowserSwitchPendingRequest.Failure(exception)
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
    fun `handleReturnToAppFromBrowser on browser switch result returns success result`() {
        val browserSwitchResultInfo: BrowserSwitchResultInfo = mockk(relaxed = true)
        val browserSwitchPendingRequest = BrowserSwitchPendingRequest.Started(browserSwitchRequest)
        val pendingRequest: SEPADirectDebitPendingRequest.Started =
            SEPADirectDebitPendingRequest.Started(
                browserSwitchPendingRequest
            )
        every {
            browserSwitchClient.completeRequest(eq(browserSwitchPendingRequest), eq(intent))
        } returns BrowserSwitchResult.Success(browserSwitchResultInfo)

        val paymentAuthResult = sut.handleReturnToAppFromBrowser(pendingRequest, intent)

        assertTrue(paymentAuthResult is SEPADirectDebitPaymentAuthResult.Success)
        assertSame(
            (paymentAuthResult as SEPADirectDebitPaymentAuthResult.Success).paymentAuthInfo.browserSwitchResultInfo,
            browserSwitchResultInfo
        )
    }

    @Test
    fun `handleReturnToAppFromBrowser when no BrowserSwitchResult returns no result`() {
        val browserSwitchPendingRequest = BrowserSwitchPendingRequest.Started(browserSwitchRequest)
        val pendingRequest: SEPADirectDebitPendingRequest.Started =
            SEPADirectDebitPendingRequest.Started(
                browserSwitchPendingRequest
            )
        every {
            browserSwitchClient.completeRequest(eq(browserSwitchPendingRequest), eq(intent))
        } returns BrowserSwitchResult.NoResult

        val paymentAuthResult = sut.handleReturnToAppFromBrowser(pendingRequest, intent)

        assertTrue(paymentAuthResult is SEPADirectDebitPaymentAuthResult.NoResult)
    }
}
