package com.braintreepayments.api.paypal

import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import com.braintreepayments.api.BrowserSwitchClient
import com.braintreepayments.api.BrowserSwitchException
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.BrowserSwitchStartResult
import com.braintreepayments.api.core.AnalyticsClient
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.GetReturnLinkUseCase
import com.braintreepayments.api.core.MerchantRepository
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    private val analyticsClient: AnalyticsClient = mockk(relaxed = true)
    private val merchantRepository = mockk<MerchantRepository>(relaxed = true)
    private val payPalTokenResponseRepository = mockk<PayPalTokenResponseRepository>(relaxed = true)
    private val getReturnLinkUseCase = mockk<GetReturnLinkUseCase>()
    private val getPaymentTokenUseCase = mockk<PayPalGetPaymentTokenUseCase>()
    private val returnUrl = "https://return.url"
    private val deepLinkScheme = "deepLinkScheme"
    private val paymentToken = "paymentToken"

    private lateinit var sut: PayPalLauncher

    @Before
    fun setup() {
        every { paymentAuthRequestParams.browserSwitchOptions } returns options

        val appSwitchReturnUrl = Uri.parse(returnUrl)
        every { getReturnLinkUseCase() } returns GetReturnLinkUseCase.ReturnLinkResult.AppLink(
            appSwitchReturnUrl
        )
        every { getPaymentTokenUseCase() } returns paymentToken
        sut = PayPalLauncher(
            browserSwitchClient = browserSwitchClient,
            merchantRepository = merchantRepository,
            payPalTokenResponseRepository = payPalTokenResponseRepository,
            getReturnLinkUseCase = getReturnLinkUseCase,
            payPalGetPaymentTokenUseCase = getPaymentTokenUseCase,
            lazyAnalyticsClient = lazy { analyticsClient })
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
    fun `handleReturnToApp sends started event`() {
        sut.handleReturnToApp(
            PayPalPendingRequest.Started(pendingRequestString),
            intent
        )
        verify {
            analyticsClient.sendEvent(
                PayPalAnalytics.HANDLE_RETURN_STARTED,
                AnalyticsEventParams(payPalContextId = paymentToken, appSwitchUrl = returnUrl)
            )
        }
    }

    @Test
    @Throws(JSONException::class)
    fun `handleReturnToApp with deeplinkScheme sends handle started event with deeplink scheme`() {
        every { getReturnLinkUseCase() } returns GetReturnLinkUseCase.ReturnLinkResult.DeepLink(
            deepLinkScheme
        )
        sut.handleReturnToApp(
            PayPalPendingRequest.Started(pendingRequestString),
            intent
        )
        verify {
            analyticsClient.sendEvent(
                PayPalAnalytics.HANDLE_RETURN_STARTED,
                AnalyticsEventParams(appSwitchUrl = deepLinkScheme, payPalContextId = paymentToken)
            )
        }
    }

    @Test
    @Throws(JSONException::class)
    fun `handleReturnToApp with ReturnLinkResult Failure sends handle started event with null appSwitchUrl`() {
        every { getReturnLinkUseCase() } returns GetReturnLinkUseCase.ReturnLinkResult.Failure(
            Exception("handle return start failed")
        )
        val slot1 = CapturingSlot<String>()
        val slot2 = CapturingSlot<AnalyticsEventParams>()
        every { analyticsClient.sendEvent(capture(slot1), capture(slot2)) } returns Unit

        sut.handleReturnToApp(
            PayPalPendingRequest.Started(pendingRequestString),
            intent
        )
        verify {
            analyticsClient.sendEvent(any(), any())
        }

        assertSame(PayPalAnalytics.HANDLE_RETURN_FAILED, slot1.captured)
        assertSame(paymentToken, slot2.captured.payPalContextId)
        assertSame(null, slot2.captured.appSwitchUrl)
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

        val slot1 = CapturingSlot<String>()
        val slot2 = CapturingSlot<AnalyticsEventParams>()
        every { analyticsClient.sendEvent(capture(slot1), capture(slot2)) } returns Unit

        val paymentAuthResult = sut.handleReturnToApp(
            PayPalPendingRequest.Started(pendingRequestString),
            intent
        )

        assertTrue(paymentAuthResult is PayPalPaymentAuthResult.Success)
        assertSame(
            browserSwitchFinalResult,
            (paymentAuthResult as PayPalPaymentAuthResult.Success).browserSwitchSuccess
        )

        verify { analyticsClient.sendEvent(any(), any()) }
        verify { analyticsClient.sendEvent(any(), any()) }

        assertSame(PayPalAnalytics.HANDLE_RETURN_SUCCEEDED, slot1.captured)
        assertSame(paymentToken, slot2.captured.payPalContextId)
        assertSame(returnUrl, slot2.captured.appSwitchUrl)
    }

    @Test
    @Throws(JSONException::class)
    fun `handleReturnToApp when result fails returns failed result`() {
        val browserSwitchFinalResult = mockk<BrowserSwitchFinalResult.Failure>()
        every {
            browserSwitchClient.completeRequest(intent, pendingRequestString)
        } returns browserSwitchFinalResult

        val slot1 = CapturingSlot<String>()
        val slot2 = CapturingSlot<AnalyticsEventParams>()
        every { analyticsClient.sendEvent(capture(slot1), capture(slot2)) } returns Unit

        val exception = BrowserSwitchException("BrowserSwitchException")
        every { browserSwitchFinalResult.error } returns exception

        val paymentAuthResult = sut.handleReturnToApp(
            PayPalPendingRequest.Started(pendingRequestString),
            intent
        )

        assertTrue(paymentAuthResult is PayPalPaymentAuthResult.Failure)
        assertSame(
            exception,
            (paymentAuthResult as PayPalPaymentAuthResult.Failure).error
        )
        verify {
            analyticsClient.sendEvent(any(), any())
        }

        assertSame(PayPalAnalytics.HANDLE_RETURN_FAILED, slot1.captured)
        assertSame(paymentToken, slot2.captured.payPalContextId)
    }

    @Test
    @Throws(JSONException::class)
    fun `handleReturnToApp when result does not exist returns null`() {
        every {
            browserSwitchClient.completeRequest(intent, pendingRequestString)
        } returns BrowserSwitchFinalResult.NoResult

        val slot1 = CapturingSlot<String>()
        val slot2 = CapturingSlot<AnalyticsEventParams>()
        every { analyticsClient.sendEvent(capture(slot1), capture(slot2)) } returns Unit

        val paymentAuthResult = sut.handleReturnToApp(
            PayPalPendingRequest.Started(pendingRequestString),
            intent
        )

        assertTrue(paymentAuthResult is PayPalPaymentAuthResult.NoResult)
        verify { analyticsClient.sendEvent(any(), any()) }

        assertSame(PayPalAnalytics.HANDLE_RETURN_NO_RESULT, slot1.captured)
        assertSame(paymentToken, slot2.captured.payPalContextId)
    }
}
