package com.braintreepayments.api

import android.content.Intent
import androidx.activity.ComponentActivity
import io.mockk.every
import io.mockk.mockk
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
    fun `handleReturnToAppFromBrowser on browser switch result returns result`() {
        val browserSwitchResult = Mockito.mock(
            BrowserSwitchResult::class.java
        )
        val browserSwitchPendingRequest = BrowserSwitchPendingRequest.Started(browserSwitchRequest)
        val pendingRequest: SEPADirectDebitPendingRequest.Started =
            SEPADirectDebitPendingRequest.Started(
                browserSwitchPendingRequest
            )
        every {
            browserSwitchClient.parseResult(eq(browserSwitchPendingRequest), eq(intent))
        } returns browserSwitchResult

        val paymentAuthResult = sut.handleReturnToAppFromBrowser(pendingRequest, intent)

        assertNotNull(paymentAuthResult)
        assertSame(paymentAuthResult!!.browserSwitchResult, browserSwitchResult)
    }

    @Test
    fun `handleReturnToAppFromBrowser when no BrowserSwitchResult returns null`() {
        val browserSwitchPendingRequest = BrowserSwitchPendingRequest.Started(browserSwitchRequest)
        val pendingRequest: SEPADirectDebitPendingRequest.Started =
            SEPADirectDebitPendingRequest.Started(
                browserSwitchPendingRequest
            )
        every {
            browserSwitchClient.parseResult(eq(browserSwitchPendingRequest), eq(intent))
        } returns null

        val paymentAuthResult = sut.handleReturnToAppFromBrowser(pendingRequest, intent)

        assertNull(paymentAuthResult)
    }
}
