package com.braintreepayments.api.paypal

import android.content.Context
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
import io.mockk.verifyOrder
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    private val pendingRequestStore: PayPalPendingRequestStore = mockk(relaxed = true)
    private val context: Context = mockk(relaxed = true)

    private lateinit var sut: PayPalLauncher

    @Before
    fun setup() {
        every { resolvePayPalUseCase() } returns false

        every { paymentAuthRequestParams.browserSwitchOptions } returns options
        every { paymentAuthRequestParams.contextId } returns paymentToken
        every { paymentAuthRequestParams.approvalUrl } returns approvalUrl
        every { paymentAuthRequestParams.shouldRequestBillingAgreement } returns isBillingAgreement
        every { paymentAuthRequestParams.recurringBillingPlanType } returns recurringBillingPlanType
        every { intent.data } returns Uri.parse(approvalUrl)
        every { options.url } returns Uri.parse(approvalUrl)

        sut = PayPalLauncher(
            browserSwitchClient = browserSwitchClient,
            getAppSwitchUseCase = getAppSwitchUseCase,
            resolvePayPalUseCase = resolvePayPalUseCase,
            lazyAnalyticsClient = lazy { analyticsClient },
            analyticsParamRepository = analyticsParamRepository,
            pendingRequestStoreProvider = { pendingRequestStore }
        )
    }

    @Test
    fun `when launch is called, browser switch is started and pending request is returned`() {
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
    fun `when browser switch start fails, launch returns pending request failure`() {
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
    fun `when device can't perform browser switch, launch returns pending request failure`() {
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
    fun `launch includes isVaultRequest true in analytics events when request is vault`(
        @TestParameter isAppSwitch: Boolean
    ) {
        every { paymentAuthRequestParams.isVaultRequest } returns true
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
                    isVaultRequest = true,
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
                    isVaultRequest = true,
                )
            )
        }
    }

    @Test
    fun `launch includes isVaultRequest false in analytics events when request is not vault`(
        @TestParameter isAppSwitch: Boolean
    ) {
        every { paymentAuthRequestParams.isVaultRequest } returns false
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
                    isVaultRequest = false,
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
                    isVaultRequest = false,
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
    fun `when a browser switch result exists, handleReturnToApp returns result`() {
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
        verify { analyticsParamRepository.shouldRequestBillingAgreement = isBillingAgreement }
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
    fun `when no browser switch result exists, handleReturnToApp returns NoResult`() {
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

    // Pending request persistence (process death recovery)

    @Test
    fun `when launch starts successfully, the pending request is persisted`() {
        every { browserSwitchClient.start(activity, options, any()) } returns
            BrowserSwitchStartResult.Started(pendingRequestString)

        sut.launch(activity, PayPalPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        verify { pendingRequestStore.save(pendingRequestString) }
    }

    @Test
    fun `when launch fails, the pending request is not persisted`() {
        every { browserSwitchClient.start(activity, options, any()) } returns
            BrowserSwitchStartResult.Failure(BrowserSwitchException("error"))

        sut.launch(activity, PayPalPaymentAuthRequest.ReadyToLaunch(paymentAuthRequestParams))

        verify(exactly = 0) { pendingRequestStore.save(any()) }
    }

    @Test
    fun `handleReturnToApp with context restores the stored pending request and returns success`() {
        every { pendingRequestStore.get() } returns pendingRequestString
        val browserSwitchFinalResult = mockk<BrowserSwitchFinalResult.Success>()
        every { browserSwitchClient.completeRequest(intent, pendingRequestString) } returns
            browserSwitchFinalResult

        val result = sut.handleReturnToApp(context, intent)

        assertTrue(result is PayPalPaymentAuthResult.Success)
        assertSame(
            browserSwitchFinalResult,
            (result as PayPalPaymentAuthResult.Success).browserSwitchSuccess
        )
        verifyOrder {
            browserSwitchClient.restorePendingRequest(pendingRequestString)
            browserSwitchClient.completeRequest(intent, pendingRequestString)
            pendingRequestStore.clear()
        }
        verify {
            analyticsClient.sendEvent(
                PayPalAnalytics.HANDLE_RETURN_PENDING_REQUEST_RESTORED,
                AnalyticsEventParams(contextId = paymentToken, appSwitchUrl = approvalUrl)
            )
        }
    }

    @Test
    fun `handleReturnToApp with context clears the stored pending request on failure`() {
        every { pendingRequestStore.get() } returns pendingRequestString
        val browserSwitchFinalResult = mockk<BrowserSwitchFinalResult.Failure>()
        val exception = BrowserSwitchException("error")
        every { browserSwitchFinalResult.error } returns exception
        every { browserSwitchClient.completeRequest(intent, pendingRequestString) } returns
            browserSwitchFinalResult

        val result = sut.handleReturnToApp(context, intent)

        assertTrue(result is PayPalPaymentAuthResult.Failure)
        assertSame(exception, (result as PayPalPaymentAuthResult.Failure).error)
        verify { pendingRequestStore.clear() }
    }

    @Test
    fun `handleReturnToApp with context keeps the stored pending request on no result`() {
        every { pendingRequestStore.get() } returns pendingRequestString
        every { browserSwitchClient.completeRequest(intent, pendingRequestString) } returns
            BrowserSwitchFinalResult.NoResult

        val result = sut.handleReturnToApp(context, intent)

        assertSame(PayPalPaymentAuthResult.NoResult, result)
        verify(exactly = 0) { pendingRequestStore.clear() }
    }

    @Test
    fun `handleReturnToApp with context returns no result when nothing is stored`() {
        every { pendingRequestStore.get() } returns null

        val result = sut.handleReturnToApp(context, intent)

        assertSame(PayPalPaymentAuthResult.NoResult, result)
        verify(exactly = 0) { browserSwitchClient.completeRequest(any(), any()) }
        verify {
            analyticsClient.sendEvent(
                PayPalAnalytics.HANDLE_RETURN_NO_STORED_PENDING_REQUEST,
                AnalyticsEventParams(contextId = paymentToken, appSwitchUrl = approvalUrl)
            )
        }
    }

    @Test
    fun `handleReturnToApp with context and no intent data does not send analytics when nothing is stored`() {
        every { pendingRequestStore.get() } returns null
        every { intent.data } returns null

        val result = sut.handleReturnToApp(context, intent)

        assertSame(PayPalPaymentAuthResult.NoResult, result)
        verify(exactly = 0) { analyticsClient.sendEvent(any(), any()) }
    }

    @Test
    fun `handleReturnToApp with context returns failure and clears store when stored request is invalid`() {
        every { pendingRequestStore.get() } returns pendingRequestString
        val exception = BrowserSwitchException("invalid")
        every { browserSwitchClient.restorePendingRequest(pendingRequestString) } throws exception

        val result = sut.handleReturnToApp(context, intent)

        assertTrue(result is PayPalPaymentAuthResult.Failure)
        assertSame(exception, (result as PayPalPaymentAuthResult.Failure).error)
        verify { pendingRequestStore.clear() }
        verify(exactly = 0) { browserSwitchClient.completeRequest(any(), any()) }
    }

    @Test
    fun `getStoredPendingRequest returns the stored pending request`() {
        every { pendingRequestStore.get() } returns pendingRequestString

        val result = sut.getStoredPendingRequest(context)

        assertEquals(pendingRequestString, result?.pendingRequestString)
    }

    @Test
    fun `getStoredPendingRequest returns null when nothing is stored`() {
        every { pendingRequestStore.get() } returns null

        assertNull(sut.getStoredPendingRequest(context))
    }
}
