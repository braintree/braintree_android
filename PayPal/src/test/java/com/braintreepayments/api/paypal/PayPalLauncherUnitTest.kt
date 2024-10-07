package com.braintreepayments.api.paypal

import android.content.Intent
import androidx.activity.ComponentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchStartResult
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.paypal.vaultedit.PayPalVaultEditAuthResult
import com.braintreepayments.api.paypal.vaultedit.PayPalVaultEditPendingRequest
import io.mockk.every
import io.mockk.mockk
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalLauncherUnitTest {
    private val browserSwitchClient: BrowserSwitchClient = mockk(relaxed = true)
    private val activity: ComponentActivity = mockk(relaxed = true)
    private val paymentAuthRequestParams: PayPalPaymentAuthRequestParams = mockk(relaxed = true)
    private val intent: Intent = mockk(relaxed = true)
    private val options: BrowserSwitchOptions = mockk(relaxed = true)
    private val pendingRequestString = "pending_request_string"

    private lateinit var sut: PayPalLauncher

    @Before
    fun setup() {
        every { paymentAuthRequestParams.browserSwitchOptions } returns options
        sut = PayPalLauncher(browserSwitchClient)
    }

    @Test
    fun `launch starts browser switch and returns pending request`() {
        val startedPendingRequest = BrowserSwitchStartResult.Started(pendingRequestString)
        every { browserSwitchClient.start(activity, options) } returns startedPendingRequest

        val pendingRequest =
            sut.launch(activity, PayPalPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        assertTrue(pendingRequest is PayPalPendingRequest.Started)
        assertEquals(
            (startedPendingRequest as BrowserSwitchStartResult.Started).pendingRequest,
            (pendingRequest as PayPalPendingRequest.Started).pendingRequestString
        )
    }

    @Test
    fun `launch on error returns pending request failure`() {
        every { paymentAuthRequestParams.browserSwitchOptions } returns options
        val exception = BrowserSwitchException("error")
        every { browserSwitchClient.start(eq(activity), eq(options)) } returns
            BrowserSwitchStartResult.Failure(exception)

        val pendingRequest =
            sut.launch(activity, PayPalPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        assertTrue(pendingRequest is PayPalPendingRequest.Failure)
        assertSame(exception, (pendingRequest as PayPalPendingRequest.Failure).error)
    }

    @Test
    @Throws(BrowserSwitchException::class)
    fun `launch when device cant perform browser switch returns pending request failure`() {
        every { paymentAuthRequestParams.browserSwitchOptions } returns options
        val exception = BrowserSwitchException("browser switch error")
        every {
            browserSwitchClient.assertCanPerformBrowserSwitch(
                eq(activity),
                eq(options)
            )
        } throws exception

        val pendingRequest =
            sut.launch(activity, PayPalPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        assertTrue(pendingRequest is PayPalPendingRequest.Failure)
        assertEquals(
            "AndroidManifest.xml is incorrectly configured or another app " +
                "defines the same browser switch url as this app. See " +
                "https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/" +
                "android/v4#browser-switch-setup " +
                "for the correct configuration: browser switch error",
            (pendingRequest as PayPalPendingRequest.Failure).error.message
        )
    }

    @Test
    @Throws(JSONException::class)
    fun `handleReturnToApp when result exists returns result`() {
        val browserSwitchFinalResult = mockk<BrowserSwitchFinalResult.Success>()
        every {
            browserSwitchClient.completeRequest(
                intent,
                pendingRequestString
            )
        } returns browserSwitchFinalResult

        val paymentAuthResult = sut.handleReturnToApp(
            PayPalPendingRequest.Started(pendingRequestString), intent
        )

        assertTrue(paymentAuthResult is PayPalPaymentAuthResult.Success)
        assertSame(
            browserSwitchFinalResult,
            (paymentAuthResult as PayPalPaymentAuthResult.Success).browserSwitchSuccess
        )
    }

    @Test
    @Throws(JSONException::class)
    fun `handleReturnToApp when result does not exist returns null`() {
        every {
            browserSwitchClient.completeRequest(intent, pendingRequestString)
        } returns BrowserSwitchFinalResult.NoResult

        val paymentAuthResult = sut.handleReturnToApp(
            PayPalPendingRequest.Started(pendingRequestString), intent
        )

        assertTrue(paymentAuthResult is PayPalPaymentAuthResult.NoResult)
    }

    @OptIn(ExperimentalBetaApi::class)
    @Test
    @Throws(JSONException::class)
    fun `handleReturnToApp with EditAuthResult when result exists returns result`() {
        val browserSwitchFinalResult = mockk<BrowserSwitchFinalResult.Success>()
        every {
            browserSwitchClient.completeRequest(
                intent,
                pendingRequestString
            )
        } returns browserSwitchFinalResult

        val editAuthResult = sut.handleReturnToApp(
            PayPalVaultEditPendingRequest.Started(pendingRequestString), intent
        )

        assertTrue(editAuthResult is PayPalVaultEditAuthResult.Success)
        assertSame(
            browserSwitchFinalResult,
            (editAuthResult as PayPalVaultEditAuthResult.Success).browserSwitchSuccess
        )
    }

    @OptIn(ExperimentalBetaApi::class)
    @Test
    @Throws(JSONException::class)
    fun `handleReturnToApp with EditAuthResult when result does not exist returns null`() {
        every {
            browserSwitchClient.completeRequest(intent, pendingRequestString)
        } returns BrowserSwitchFinalResult.NoResult

        val editAuthResult = sut.handleReturnToApp(
            PayPalVaultEditPendingRequest.Started(pendingRequestString), intent
        )

        assertTrue(editAuthResult is PayPalVaultEditAuthResult.NoResult)
    }
}
