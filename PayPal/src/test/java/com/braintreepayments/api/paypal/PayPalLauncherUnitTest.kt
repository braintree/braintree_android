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
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.usecase.GetAppSwitchUseCase
import com.google.testing.junit.testparameterinjector.TestParameter
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
import org.robolectric.RobolectricTestParameterInjector

@RunWith(RobolectricTestParameterInjector::class)
class PayPalLauncherUnitTest {
    private val browserSwitchClient: BrowserSwitchClient = mockk(relaxed = true)
    private val activity: ComponentActivity = mockk(relaxed = true)
    private val paymentAuthRequestParams: PayPalPaymentAuthRequestParams = mockk(relaxed = true)
    private val intent: Intent = mockk(relaxed = true)
    private val options: BrowserSwitchOptions = mockk(relaxed = true)
    private val pendingRequestString = "pending_request_string"
    private val analyticsClient: AnalyticsClient = mockk(relaxed = true)
    private val getAppSwitchUseCase = mockk<GetAppSwitchUseCase>(relaxed = true)
    private val resolvePayPalUseCase = mockk<ResolvePayPalUseCase>(relaxed = true)
    private val analyticsParamRepository = mockk<AnalyticsParamRepository>(relaxed = true)
    private val paymentToken = "paymentToken"
    private val approvalUrl = "https://return.url?ba_token=$paymentToken"
    private val isBillingAgreement = true
    private val isPurchase = true
    private val recurringBillingPlanType = PayPalRecurringBillingPlanType.RECURRING.name

    private lateinit var sut: PayPalLauncher

    @Before
    fun setup() {
        every { resolvePayPalUseCase() } returns false

        every { paymentAuthRequestParams.browserSwitchOptions } returns options
        every { paymentAuthRequestParams.contextId } returns paymentToken
        every { paymentAuthRequestParams.approvalUrl } returns approvalUrl
        every { paymentAuthRequestParams.isBillingAgreement } returns isBillingAgreement
        every { paymentAuthRequestParams.isPurchase } returns isPurchase
        every { paymentAuthRequestParams.recurringBillingPlanType } returns recurringBillingPlanType
        every { intent.data } returns Uri.parse(approvalUrl)
        every { options.url } returns Uri.parse(approvalUrl)

        sut = PayPalLauncher(
            browserSwitchClient = browserSwitchClient,
            getAppSwitchUseCase = getAppSwitchUseCase,
            resolvePayPalUseCase = resolvePayPalUseCase,
            lazyAnalyticsClient = lazy { analyticsClient },
            analyticsParamRepository = analyticsParamRepository
        )
    }

    @Test
    fun `launch starts browser switch and returns pending request`() {
        val startedPendingRequest = BrowserSwitchStartResult.Started(pendingRequestString)
        every { browserSwitchClient.start(activity, options, any()) } returns startedPendingRequest

        val pendingRequest =
            sut.launch(activity, PayPalPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        assertTrue(pendingRequest is PayPalPendingRequest.Started)
        assertEquals(
            startedPendingRequest.pendingRequest,
            (pendingRequest as PayPalPendingRequest.Started).pendingRequestString
        )
    }

    @Test
    fun `launch on error returns pending request failure`() {
        every { paymentAuthRequestParams.browserSwitchOptions } returns options
        val exception = BrowserSwitchException("error")
        every { browserSwitchClient.start(eq(activity), eq(options), any()) } returns
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
    fun `launch sends APP_SWITCH_STARTED and APP_SWITCH_SUCCEEDED analytics events`(
        @TestParameter isAppSwitch: Boolean
    ) {
        val startedPendingRequest = BrowserSwitchStartResult.Started(pendingRequestString)
        every { browserSwitchClient.start(activity, options, any()) } returns startedPendingRequest
        every { getAppSwitchUseCase() } returns isAppSwitch
        every { resolvePayPalUseCase() } returns isAppSwitch

        sut.launch(activity, PayPalPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        verify {
            analyticsClient.sendEvent(
                if (isAppSwitch) PayPalAnalytics.APP_SWITCH_STARTED else PayPalAnalytics.BROWSER_PRESENTATION_STARTED,
                AnalyticsEventParams(
                    contextId = paymentToken,
                    appSwitchUrl = approvalUrl,
                )
            )
        }

        verify {
            analyticsClient.sendEvent(
                if (isAppSwitch) {
                    PayPalAnalytics.APP_SWITCH_SUCCEEDED
                } else {
                    PayPalAnalytics.BROWSER_PRESENTATION_SUCCEEDED
                },
                AnalyticsEventParams(
                    contextId = paymentToken,
                    appSwitchUrl = approvalUrl,
                )
            )
        }
    }

    @Test
    fun `launch sends APP_SWITCH_FAILED analytics event when browser switch cannot be performed`(
        @TestParameter isAppSwitch: Boolean
    ) {
        every { getAppSwitchUseCase() } returns isAppSwitch
        every { resolvePayPalUseCase() } returns isAppSwitch
        val exception = BrowserSwitchException("browser switch error")
        every {
            browserSwitchClient.assertCanPerformBrowserSwitch(eq(activity), eq(options))
        } throws exception

        sut.launch(activity, PayPalPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        verify {
            analyticsClient.sendEvent(
                if (isAppSwitch) PayPalAnalytics.APP_SWITCH_STARTED else PayPalAnalytics.BROWSER_PRESENTATION_STARTED,
                AnalyticsEventParams(
                    contextId = paymentToken,
                    appSwitchUrl = approvalUrl,
                )
            )
        }

        verify {
            analyticsClient.sendEvent(
                if (isAppSwitch) PayPalAnalytics.APP_SWITCH_FAILED else PayPalAnalytics.BROWSER_PRESENTATION_FAILED,
                AnalyticsEventParams(
                    contextId = paymentToken,
                    appSwitchUrl = approvalUrl,
                    errorDescription = "AndroidManifest.xml is incorrectly configured or another app " +
                        "defines the same browser switch url as this app. See " +
                        "https://developer.paypal.com/braintree/docs/guides/client-sdk/setup/" +
                        "android/v4#browser-switch-setup for the correct configuration: browser switch error",
                )
            )
        }
    }

    @Test
    fun `launch sends APP_SWITCH_FAILED analytics event when browserSwitchOptions is null`() {
        every { getAppSwitchUseCase() } returns true
        every { resolvePayPalUseCase() } returns true
        every { paymentAuthRequestParams.browserSwitchOptions } returns null

        sut.launch(activity, PayPalPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        verify {
            analyticsClient.sendEvent(
                PayPalAnalytics.APP_SWITCH_STARTED,
                AnalyticsEventParams(
                    contextId = paymentToken,
                    appSwitchUrl = approvalUrl,
                )
            )
        }

        verify {
            analyticsClient.sendEvent(
                PayPalAnalytics.APP_SWITCH_FAILED,
                AnalyticsEventParams(
                    contextId = paymentToken,
                    appSwitchUrl = approvalUrl,
                    errorDescription = "BrowserSwitchOptions is null"
                )
            )
        }
    }

    @Test
    fun `launch sends STARTED and FAILED analytics events when browserSwitchOptions URL is null`(
        @TestParameter isAppSwitch: Boolean
    ) {
        every { getAppSwitchUseCase() } returns isAppSwitch
        every { resolvePayPalUseCase() } returns isAppSwitch
        every { options.url } returns null

        val pendingRequest = sut.launch(activity, PayPalPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        assertTrue(pendingRequest is PayPalPendingRequest.Failure)
        assertEquals(
            "BrowserSwitchOptions URL is null",
            (pendingRequest as PayPalPendingRequest.Failure).error.message
        )

        verify {
            analyticsClient.sendEvent(
                if (isAppSwitch) PayPalAnalytics.APP_SWITCH_STARTED else PayPalAnalytics.BROWSER_PRESENTATION_STARTED,
                AnalyticsEventParams(
                    contextId = paymentToken,
                    appSwitchUrl = approvalUrl,
                )
            )
        }

        verify {
            analyticsClient.sendEvent(
                if (isAppSwitch) PayPalAnalytics.APP_SWITCH_FAILED else PayPalAnalytics.BROWSER_PRESENTATION_FAILED,
                AnalyticsEventParams(
                    contextId = paymentToken,
                    appSwitchUrl = approvalUrl,
                    errorDescription = "BrowserSwitchOptions URL is null"
                )
            )
        }
    }

    @Test
    @Throws(JSONException::class)
    fun `handleReturnToApp sends started event`() {
        val token = "token"
        val returnUrl = "https://return.url?token=$token"
        every { intent.data } returns Uri.parse(returnUrl)

        sut.handleReturnToApp(
            PayPalPendingRequest.Started(pendingRequestString),
            intent
        )
        verify {
            analyticsClient.sendEvent(
                PayPalAnalytics.HANDLE_RETURN_STARTED,
                AnalyticsEventParams(contextId = token, appSwitchUrl = returnUrl)
            )
        }
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
        assertEquals(paymentToken, slot2.captured.contextId)
        assertEquals(approvalUrl, slot2.captured.appSwitchUrl)
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
        assertEquals(paymentToken, slot2.captured.contextId)
    }

    @Test
    fun `launch sets didSdkAttemptAppSwitch to true when app switch conditions are met`() {
        every { getAppSwitchUseCase() } returns true
        every { resolvePayPalUseCase() } returns true
        val startedPendingRequest = BrowserSwitchStartResult.Started(pendingRequestString)
        every { browserSwitchClient.start(activity, options, any()) } returns startedPendingRequest

        sut.launch(activity, PayPalPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        verify { analyticsParamRepository.didSdkAttemptAppSwitch = true }
        verify {
            analyticsClient.sendEvent(
                PayPalAnalytics.APP_SWITCH_STARTED,
                AnalyticsEventParams(
                    contextId = paymentToken,
                    appSwitchUrl = approvalUrl,
                )
            )
        }
    }

    @Test
    fun `launch sets didSdkAttemptAppSwitch to false when PayPal cannot handle URL`() {
        every { getAppSwitchUseCase() } returns true
        every { resolvePayPalUseCase() } returns false
        val startedPendingRequest = BrowserSwitchStartResult.Started(pendingRequestString)
        every { browserSwitchClient.start(activity, options, any()) } returns startedPendingRequest

        sut.launch(activity, PayPalPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        verify { analyticsParamRepository.didSdkAttemptAppSwitch = false }
        verify {
            analyticsClient.sendEvent(
                PayPalAnalytics.BROWSER_PRESENTATION_STARTED,
                AnalyticsEventParams(
                    contextId = paymentToken,
                    appSwitchUrl = approvalUrl,
                )
            )
        }
    }

    @Test
    fun `launch sets didSdkAttemptAppSwitch to false when getAppSwitchUseCase returns false`() {
        every { getAppSwitchUseCase() } returns false
        every { resolvePayPalUseCase() } returns true
        val startedPendingRequest = BrowserSwitchStartResult.Started(pendingRequestString)
        every { browserSwitchClient.start(activity, options, any()) } returns startedPendingRequest

        sut.launch(activity, PayPalPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        verify { analyticsParamRepository.didSdkAttemptAppSwitch = false }
        verify {
            analyticsClient.sendEvent(
                PayPalAnalytics.BROWSER_PRESENTATION_STARTED,
                AnalyticsEventParams(
                    contextId = paymentToken,
                    appSwitchUrl = approvalUrl,
                )
            )
        }
    }

    @Test
    fun `launch sets fundingSource`() {
        val startedPendingRequest = BrowserSwitchStartResult.Started(pendingRequestString)
        every { browserSwitchClient.start(activity, options, any()) } returns startedPendingRequest

        sut.launch(activity, PayPalPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))
        verify { analyticsParamRepository.fundingSource = paymentAuthRequestParams.fundingSource }
    }

    @Test
    fun `launch sets isBillingAgreement`() {
        val startedPendingRequest = BrowserSwitchStartResult.Started(pendingRequestString)
        every { browserSwitchClient.start(activity, options, any()) } returns startedPendingRequest

        sut.launch(activity, PayPalPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))
        verify { analyticsParamRepository.isBillingAgreement = isBillingAgreement }
    }

    @Test
    fun `launch sets isPurchase`() {
        val startedPendingRequest = BrowserSwitchStartResult.Started(pendingRequestString)
        every { browserSwitchClient.start(activity, options, any()) } returns startedPendingRequest

        sut.launch(activity, PayPalPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))
        verify { analyticsParamRepository.isPurchase = isPurchase }
    }

    @Test
    fun `launch sets billingPlanType`() {
        val startedPendingRequest = BrowserSwitchStartResult.Started(pendingRequestString)
        every { browserSwitchClient.start(activity, options, any()) } returns startedPendingRequest

        sut.launch(activity, PayPalPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))
        verify { analyticsParamRepository.recurringBillingPlanType = recurringBillingPlanType }
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
        assertEquals(paymentToken, slot2.captured.contextId)
    }

    @Test
    fun `launch passes isAppSwitch as true to browserSwitchClient when both conditions are met`() {
        every { getAppSwitchUseCase() } returns true
        every { resolvePayPalUseCase() } returns true
        val startedPendingRequest = BrowserSwitchStartResult.Started(pendingRequestString)
        every { browserSwitchClient.start(activity, options, true) } returns startedPendingRequest

        sut.launch(activity, PayPalPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        verify { browserSwitchClient.start(activity, options, true) }
    }

    @Test
    fun `launch passes isAppSwitch as false to browserSwitchClient when forceCCT is enabled`() {
        every { getAppSwitchUseCase() } returns false
        every { resolvePayPalUseCase() } returns false
        val startedPendingRequest = BrowserSwitchStartResult.Started(pendingRequestString)
        every { browserSwitchClient.start(activity, options, false) } returns startedPendingRequest

        sut.launch(activity, PayPalPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        verify { browserSwitchClient.start(activity, options, false) }
    }
}
