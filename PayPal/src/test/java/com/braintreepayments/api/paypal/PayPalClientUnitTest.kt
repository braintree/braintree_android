package com.braintreepayments.api.paypal

import android.net.Uri
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.LaunchType
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.BraintreeRequestCodes
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.Configuration.Companion.fromJson
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.core.LinkType
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.core.usecase.GetAppLinksCompatibleBrowserUseCase
import com.braintreepayments.api.core.usecase.GetDefaultAppUseCase
import com.braintreepayments.api.core.usecase.GetReturnLinkTypeUseCase
import com.braintreepayments.api.core.usecase.GetReturnLinkTypeUseCase.ReturnLinkTypeResult
import com.braintreepayments.api.core.usecase.GetReturnLinkUseCase
import com.braintreepayments.api.core.usecase.GetReturnLinkUseCase.ReturnLinkResult
import com.braintreepayments.api.core.usecase.GetReturnLinkUseCase.ReturnLinkResult.AppLink
import com.braintreepayments.api.core.usecase.GetReturnLinkUseCase.ReturnLinkResult.DeepLink
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PayPalClientUnitTest {
    private val testDispatcher = StandardTestDispatcher()
    private val activity = mockk<FragmentActivity>(relaxed = true)
    private val payPalEnabledConfig: Configuration = fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
    private val payPalDisabledConfig: Configuration = fromJson(Fixtures.CONFIGURATION_WITH_DISABLED_PAYPAL)

    private val payPalTokenizeCallback = mockk<PayPalTokenizeCallback>(relaxed = true)
    private val paymentAuthCallback: PayPalPaymentAuthCallback = mockk(relaxed = true)

    private val merchantRepository: MerchantRepository = mockk(relaxed = true)
    private val getDefaultAppUseCase: GetDefaultAppUseCase = mockk(relaxed = true)
    private val getAppLinksCompatibleBrowserUseCase: GetAppLinksCompatibleBrowserUseCase = mockk(relaxed = true)
    private val getReturnLinkTypeUseCase: GetReturnLinkTypeUseCase =
        mockk<GetReturnLinkTypeUseCase>(relaxed = true)
    private val getReturnLinkUseCase: GetReturnLinkUseCase = mockk(relaxed = true)
    private val analyticsParamRepository: AnalyticsParamRepository = mockk(relaxed = true)

    @Before
    @Throws(JSONException::class)
    fun beforeEach() {
        every { merchantRepository.returnUrlScheme } returns "com.braintreepayments.demo"
        every { getReturnLinkUseCase.invoke() } returns AppLink(Uri.parse("www.example.com"))
        every { getReturnLinkTypeUseCase.invoke() } returns ReturnLinkTypeResult.APP_LINK
    }

    @Test
    fun initialization_sets_app_link_in_analyticsParamRepository() {
        val payPalVaultRequest = PayPalVaultRequest(true)
        val paymentAuthRequest = PayPalPaymentAuthRequestParams(
            payPalVaultRequest,
            null,
            "https://example.com/approval/url",
            "sample-client-metadata-id",
            null,
            "https://example.com/success/url"
        )

        val payPalInternalClient = MockkPayPalInternalClientBuilder()
            .sendRequestSuccess(paymentAuthRequest)
            .build()

        val braintreeClient =
            MockkBraintreeClientBuilder().configurationSuccess(payPalEnabledConfig).build()

        testPaypalClient(
            braintreeClient,
            payPalInternalClient,
        )

        verify { analyticsParamRepository.linkType = LinkType.APP_LINK }
    }

    @Test
    fun initialization_sets_deep_link_in_analyticsParamRepository() {
        every { getReturnLinkTypeUseCase.invoke() } returns ReturnLinkTypeResult.DEEP_LINK
        val payPalVaultRequest = PayPalVaultRequest(true)
        val paymentAuthRequest = PayPalPaymentAuthRequestParams(
            payPalVaultRequest,
            null,
            "https://example.com/approval/url",
            "sample-client-metadata-id",
            null,
            "https://example.com/success/url"
        )

        val payPalInternalClient = MockkPayPalInternalClientBuilder()
            .sendRequestSuccess(paymentAuthRequest)
            .build()

        val braintreeClient =
            MockkBraintreeClientBuilder().configurationSuccess(payPalEnabledConfig).build()

        testPaypalClient(
            braintreeClient,
            payPalInternalClient,
        )

        verify { analyticsParamRepository.linkType = LinkType.DEEP_LINK }
    }

    @OptIn(ExperimentalBetaApi::class)
    @Test
    @Throws(JSONException::class)
    fun createPaymentAuthRequest_callsBackPayPalResponse_sendsStartedAnalytics() = runTest(testDispatcher) {
        val payPalVaultRequest = PayPalVaultRequest(true)
        payPalVaultRequest.merchantAccountId = "sample-merchant-account-id"
        payPalVaultRequest.shopperSessionId = "test-shopper-session-id"

        val paymentAuthRequest = PayPalPaymentAuthRequestParams(
            payPalRequest = payPalVaultRequest,
            browserSwitchOptions = null,
            approvalUrl = "https://example.com/approval/url",
            clientMetadataId = "sample-client-metadata-id",
            successUrl = "https://example.com/success/url"
        )
        val payPalInternalClient =
            MockkPayPalInternalClientBuilder().sendRequestSuccess(paymentAuthRequest).build()

        val braintreeClient =
            MockkBraintreeClientBuilder().configurationSuccess(payPalEnabledConfig).build()

        val sut = testPaypalClient(
            braintreeClient,
            payPalInternalClient,
            testDispatcher,
            this
        )
        sut.createPaymentAuthRequest(activity, payPalVaultRequest, paymentAuthCallback)
        advanceUntilIdle()

        val slot = slot<PayPalPaymentAuthRequest>()
        verify { paymentAuthCallback.onPayPalPaymentAuthRequest(capture(slot)) }

        val request = slot.captured
        assertTrue(request is PayPalPaymentAuthRequest.ReadyToLaunch)
        val paymentAuthRequestCaptured =
            (request as PayPalPaymentAuthRequest.ReadyToLaunch).requestParams

        val browserSwitchOptions = paymentAuthRequestCaptured.browserSwitchOptions
        assertEquals(
            BraintreeRequestCodes.PAYPAL.code,
            browserSwitchOptions!!.requestCode
        )
        TestCase.assertFalse(browserSwitchOptions.isLaunchAsNewTask)

        assertEquals(
            Uri.parse("https://example.com/approval/url"),
            browserSwitchOptions.url
        )

        val metadata = browserSwitchOptions.metadata
        assertEquals("https://example.com/approval/url", metadata!!.get("approval-url"))
        assertEquals("https://example.com/success/url", metadata.get("success-url"))
        assertEquals("billing-agreement", metadata.get("payment-type"))
        assertEquals("sample-client-metadata-id", metadata.get("client-metadata-id"))
        assertEquals("sample-merchant-account-id", metadata.get("merchant-account-id"))
        assertEquals("paypal-browser", metadata.get("source"))

        verify {
            braintreeClient.sendAnalyticsEvent(
                PayPalAnalytics.TOKENIZATION_STARTED,
                AnalyticsEventParams(
                    isVaultRequest = true,
                    shopperSessionId = "test-shopper-session-id"
                ),
                true
            )
        }
    }

    @Test
    fun createPaymentAuthRequest_launchesBrowserSwitchWith_ACTIVITY_CLEAR_TOP() = runTest(testDispatcher) {
        val payPalVaultRequest = PayPalVaultRequest(true)
        payPalVaultRequest.merchantAccountId = "sample-merchant-account-id"

        val paymentAuthRequest = PayPalPaymentAuthRequestParams(
            payPalVaultRequest,
            null,
            "https://example.com/approval/url",
            "sample-client-metadata-id",
            null,
            "https://example.com/success/url"
        )

        val payPalInternalClient =
            MockkPayPalInternalClientBuilder().sendRequestSuccess(paymentAuthRequest)
                .build()

        val braintreeClient =
            MockkBraintreeClientBuilder().configurationSuccess(payPalEnabledConfig)
                .launchesBrowserSwitchAsNewTask(true).build()

        val sut = testPaypalClient(
            braintreeClient,
            payPalInternalClient,
            testDispatcher,
            this
        )
        sut.createPaymentAuthRequest(activity, payPalVaultRequest, paymentAuthCallback)
        advanceUntilIdle()

        val slot = slot<PayPalPaymentAuthRequest>()
        verify { paymentAuthCallback.onPayPalPaymentAuthRequest(capture(slot)) }

        val request = slot.captured
        assertTrue(request is PayPalPaymentAuthRequest.ReadyToLaunch)
        assertTrue(
            (request as PayPalPaymentAuthRequest.ReadyToLaunch)
                .requestParams.browserSwitchOptions?.launchType == LaunchType.ACTIVITY_CLEAR_TOP
        )
    }

    @Test
    fun createPaymentAuthRequest_setsAppLinkReturnUrl() = runTest(testDispatcher) {
        every { getReturnLinkUseCase.invoke(any()) } returns AppLink("www.example.com".toUri())
        val payPalVaultRequest = PayPalVaultRequest(true)
        payPalVaultRequest.merchantAccountId = "sample-merchant-account-id"

        val paymentAuthRequest = PayPalPaymentAuthRequestParams(
            payPalVaultRequest,
            null,
            "https://example.com/approval/url",
            "sample-client-metadata-id",
            null,
            "https://example.com/success/url"
        )

        val payPalInternalClient =
            MockkPayPalInternalClientBuilder().sendRequestSuccess(paymentAuthRequest)
                .build()

        every { merchantRepository.appLinkReturnUri } returns Uri.parse("www.example.com")

        val braintreeClient = MockkBraintreeClientBuilder().configurationSuccess(payPalEnabledConfig)
            .build()

        val sut = testPaypalClient(
            braintreeClient,
            payPalInternalClient,
            testDispatcher,
            this
        )
        sut.createPaymentAuthRequest(activity, payPalVaultRequest, paymentAuthCallback)
        advanceUntilIdle()

        val slot = slot<PayPalPaymentAuthRequest>()
        verify { paymentAuthCallback.onPayPalPaymentAuthRequest(capture(slot)) }

        val request = slot.captured
        assertTrue(request is PayPalPaymentAuthRequest.ReadyToLaunch)
        assertEquals(
            merchantRepository.appLinkReturnUri,
            (request as PayPalPaymentAuthRequest.ReadyToLaunch).requestParams.browserSwitchOptions!!.appLinkUri
        )
    }

    @Test
    fun createPaymentAuthRequest_setsDeepLinkReturnUrlScheme() = runTest(testDispatcher) {
        every { getReturnLinkUseCase.invoke(any()) } returns DeepLink("com.braintreepayments.demo")
        val payPalVaultRequest = PayPalVaultRequest(true)
        payPalVaultRequest.merchantAccountId = "sample-merchant-account-id"

        val paymentAuthRequest = PayPalPaymentAuthRequestParams(
            payPalVaultRequest,
            null,
            "https://example.com/approval/url",
            "sample-client-metadata-id",
            null,
            "https://example.com/success/url"
        )

        val payPalInternalClient =
            MockkPayPalInternalClientBuilder().sendRequestSuccess(paymentAuthRequest)
                .build()

        val braintreeClient = MockkBraintreeClientBuilder().configurationSuccess(payPalEnabledConfig)
            .build()

        val sut = testPaypalClient(
            braintreeClient,
            payPalInternalClient,
            testDispatcher,
            this
        )
        sut.createPaymentAuthRequest(activity, payPalVaultRequest, paymentAuthCallback)
        advanceUntilIdle()

        val slot = slot<PayPalPaymentAuthRequest>()
        verify { paymentAuthCallback.onPayPalPaymentAuthRequest(capture(slot)) }

        val request = slot.captured
        assertTrue(request is PayPalPaymentAuthRequest.ReadyToLaunch)
        assertEquals(
            "com.braintreepayments.demo",
            (request as PayPalPaymentAuthRequest.ReadyToLaunch).requestParams.browserSwitchOptions!!.returnUrlScheme
        )
    }

    @Test
    fun createPaymentAuthRequest_returnsAnErrorWhen_getReturnLinkUseCase_returnsAFailure() = runTest(testDispatcher) {
        val exception = BraintreeException()
        every { getReturnLinkUseCase.invoke(any()) } returns ReturnLinkResult.Failure(exception)

        val payPalVaultRequest = PayPalVaultRequest(true)
        payPalVaultRequest.merchantAccountId = "sample-merchant-account-id"

        val paymentAuthRequest = PayPalPaymentAuthRequestParams(
            payPalVaultRequest,
            null,
            "https://example.com/approval/url",
            "sample-client-metadata-id",
            null,
            "https://example.com/success/url"
        )

        val payPalInternalClient =
            MockkPayPalInternalClientBuilder().sendRequestSuccess(paymentAuthRequest)
                .build()

        val braintreeClient = MockkBraintreeClientBuilder().configurationSuccess(payPalEnabledConfig)
            .build()

        val sut = testPaypalClient(
            braintreeClient,
            payPalInternalClient,
            testDispatcher,
            this
        )
        sut.createPaymentAuthRequest(activity, payPalVaultRequest, paymentAuthCallback)
        advanceUntilIdle()

        val slot = slot<PayPalPaymentAuthRequest>()
        verify { paymentAuthCallback.onPayPalPaymentAuthRequest(capture(slot)) }

        val request = slot.captured
        assertTrue(request is PayPalPaymentAuthRequest.Failure)
        assertEquals(exception, (request as PayPalPaymentAuthRequest.Failure).error)
    }

    @Test
    fun createPaymentAuthRequest_whenPayPalNotEnabled_returnsError() = runTest(testDispatcher) {
        val payPalInternalClient = MockkPayPalInternalClientBuilder().build()

        val braintreeClient =
            MockkBraintreeClientBuilder().configurationSuccess(payPalDisabledConfig).build()

        val sut = testPaypalClient(
            braintreeClient,
            payPalInternalClient,
            testDispatcher,
            this
        )
        sut.createPaymentAuthRequest(
            activity, PayPalCheckoutRequest("1.00", true),
            paymentAuthCallback
        )
        advanceUntilIdle()

        val slot = slot<PayPalPaymentAuthRequest>()
        verify { paymentAuthCallback.onPayPalPaymentAuthRequest(capture(slot)) }

        val request = slot.captured
        assertTrue(request is PayPalPaymentAuthRequest.Failure)
        assertEquals(
            PayPalClient.Companion.PAYPAL_NOT_ENABLED_MESSAGE,
            (request as PayPalPaymentAuthRequest.Failure).error.message
        )

        val params = AnalyticsEventParams(
            isVaultRequest = false,
            errorDescription = PayPalClient.Companion.PAYPAL_NOT_ENABLED_MESSAGE
        )
        verify { braintreeClient.sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_FAILED, params, true) }
        verify { analyticsParamRepository.reset() }
    }

    @Test
    fun createPaymentAuthRequest_whenCheckoutRequest_whenConfigError_forwardsErrorToListener() = runTest(testDispatcher) {
        val payPalInternalClient = MockkPayPalInternalClientBuilder().build()

        val errorMessage = "Error fetching auth"
        val authError = IOException(errorMessage)
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationError(authError)
            .build()

        val sut = testPaypalClient(
            braintreeClient,
            payPalInternalClient,
            testDispatcher,
            this
        )
        sut.createPaymentAuthRequest(
            activity,
            PayPalCheckoutRequest("1.00", true),
            paymentAuthCallback
        )
        advanceUntilIdle()

        val slot = slot<PayPalPaymentAuthRequest>()
        verify { paymentAuthCallback.onPayPalPaymentAuthRequest(capture(slot)) }

        val request = slot.captured
        assertTrue(request is PayPalPaymentAuthRequest.Failure)
        assertEquals(authError, (request as PayPalPaymentAuthRequest.Failure).error)

        val params = AnalyticsEventParams(
            isVaultRequest = false,
            errorDescription = errorMessage
        )
        verify { braintreeClient.sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_FAILED, params, true) }
    }

    @Test
    fun requestBillingAgreement_whenConfigError_forwardsErrorToListener() = runTest(testDispatcher) {
        val payPalInternalClient = MockkPayPalInternalClientBuilder().build()

        val errorMessage = "Error fetching auth"
        val authError = IOException(errorMessage)
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationError(authError)
            .build()

        val sut = testPaypalClient(
            braintreeClient,
            payPalInternalClient,
            testDispatcher,
            this
        )
        sut.createPaymentAuthRequest(activity, PayPalVaultRequest(true), paymentAuthCallback)
        advanceUntilIdle()

        val slot = slot<PayPalPaymentAuthRequest>()
        verify { paymentAuthCallback.onPayPalPaymentAuthRequest(capture(slot)) }

        val request = slot.captured
        assertTrue(request is PayPalPaymentAuthRequest.Failure)
        assertEquals(authError, (request as PayPalPaymentAuthRequest.Failure).error)

        val params = AnalyticsEventParams(
            isVaultRequest = true,
            errorDescription = errorMessage
        )
        verify { braintreeClient.sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_FAILED, params, true) }
    }

    @Test
    fun createPaymentAuthRequest_sets_analyticsParamRepository_didEnablePayPalAppSwitch() = runTest(testDispatcher) {
        val payPalInternalClient = MockkPayPalInternalClientBuilder().build()

        val braintreeClient =
            MockkBraintreeClientBuilder().configurationSuccess(payPalEnabledConfig).build()

        val payPalRequest = PayPalVaultRequest(
            hasUserLocationConsent = true,
            shouldOfferCredit = false,
            recurringBillingDetails = null,
            recurringBillingPlanType = null,
            enablePayPalAppSwitch = true
        )

        val sut = testPaypalClient(
            braintreeClient,
            payPalInternalClient,
            testDispatcher,
            this
        )
        sut.createPaymentAuthRequest(activity, payPalRequest, paymentAuthCallback)
        advanceUntilIdle()

        verify { analyticsParamRepository.didEnablePayPalAppSwitch = true }
    }

    @Test
    fun createPaymentAuthRequest_whenVaultRequest_sendsPayPalRequestViaInternalClient() = runTest(testDispatcher) {
        val payPalInternalClient = MockkPayPalInternalClientBuilder().build()

        val braintreeClient =
            MockkBraintreeClientBuilder().configurationSuccess(payPalEnabledConfig).build()

        val payPalRequest = PayPalVaultRequest(true)

        val sut = testPaypalClient(
            braintreeClient,
            payPalInternalClient,
            testDispatcher,
            this
        )
        sut.createPaymentAuthRequest(activity, payPalRequest, paymentAuthCallback)
        advanceUntilIdle()

        verify {
            payPalInternalClient.sendRequest(
                activity,
                payPalRequest,
                any<Configuration>(),
                any<PayPalInternalClientCallback>()
            )
        }
    }

    @Test
    fun createPaymentAuthRequest_whenCheckoutRequest_sendsPayPalRequestViaInternalClient() = runTest(testDispatcher) {
        val payPalInternalClient = MockkPayPalInternalClientBuilder().build()

        val braintreeClient =
            MockkBraintreeClientBuilder().configurationSuccess(payPalEnabledConfig).build()

        val payPalRequest = PayPalCheckoutRequest("1.00", true)

        val sut = testPaypalClient(
            braintreeClient,
            payPalInternalClient,
            testDispatcher,
            this
        )
        sut.createPaymentAuthRequest(activity, payPalRequest, paymentAuthCallback)
        advanceUntilIdle()

        verify {
            payPalInternalClient.sendRequest(
                activity,
                payPalRequest,
                any<Configuration>(),
                any<PayPalInternalClientCallback>()
            )
        }
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_withBillingAgreement_tokenizesResponseOnSuccess() {
        val payPalInternalClient = MockkPayPalInternalClientBuilder().build()
        val approvalUrl =
            "sample-scheme://onetouch/v1/success?" +
                "PayerID=HERMES-SANDBOX-PAYER-ID" +
                "&paymentId=HERMES-SANDBOX-PAYMENT-ID" +
                "&ba_token=EC-HERMES-SANDBOX-EC-TOKEN"
        val browserSwitchResult = mockk<BrowserSwitchFinalResult.Success>(relaxed = true)

        every { browserSwitchResult.requestMetadata } returns
            JSONObject().put("client-metadata-id", "sample-client-metadata-id")
                .put("merchant-account-id", "sample-merchant-account-id")
                .put("intent", "authorize").put("approval-url", approvalUrl)
                .put("success-url", "https://example.com/success")
                .put("payment-type", "billing-agreement")

        val uri = Uri.parse(approvalUrl)
        every { browserSwitchResult.returnUrl } returns uri

        val payPalPaymentAuthResult = PayPalPaymentAuthResult.Success(browserSwitchResult)
        val braintreeClient = MockkBraintreeClientBuilder().build()
        val sut = testPaypalClient(
            braintreeClient,
            payPalInternalClient,
        )

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback)

        val slot = slot<PayPalAccount>()
        verify {
            payPalInternalClient.tokenize(
                capture(slot),
                any<PayPalInternalTokenizeCallback>()
            )
        }

        val payPalAccount = slot.captured
        val tokenizePayload = payPalAccount.buildJSON()
        assertEquals(
            "sample-merchant-account-id",
            tokenizePayload.get("merchant_account_id")
        )

        val payPalTokenizePayload = tokenizePayload.getJSONObject("paypalAccount")
        val expectedPayPalTokenizePayload =
            JSONObject().put("correlationId", "sample-client-metadata-id")
                .put("client", JSONObject())
                .put("response", JSONObject().put("webURL", approvalUrl))
                .put("intent", "authorize").put("response_type", "web")

        JSONAssert.assertEquals(expectedPayPalTokenizePayload, payPalTokenizePayload, true)
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_withOneTimePayment_tokenizesResponseOnSuccess() {
        val payPalInternalClient = MockkPayPalInternalClientBuilder().build()
        val approvalUrl =
            "sample-scheme://onetouch/v1/success?" +
                "PayerID=HERMES-SANDBOX-PAYER-ID" +
                "&paymentId=HERMES-SANDBOX-PAYMENT-ID" +
                "&token=EC-HERMES-SANDBOX-EC-TOKEN"

        val browserSwitchResult = mockk<BrowserSwitchFinalResult.Success>(relaxed = true)

        every { browserSwitchResult.requestMetadata } returns
            JSONObject().put("client-metadata-id", "sample-client-metadata-id")
                .put("merchant-account-id", "sample-merchant-account-id")
                .put("intent", "authorize").put("approval-url", approvalUrl)
                .put("success-url", "https://example.com/success")
                .put("payment-type", "single-payment")

        val uri = Uri.parse(approvalUrl)
        every { browserSwitchResult.returnUrl } returns uri

        val payPalPaymentAuthResult = PayPalPaymentAuthResult.Success(browserSwitchResult)
        val braintreeClient = MockkBraintreeClientBuilder().build()
        val sut = testPaypalClient(
            braintreeClient,
            payPalInternalClient,
        )

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback)

        val slot = slot<PayPalAccount>()
        verify {
            payPalInternalClient.tokenize(
                capture(slot),
                any<PayPalInternalTokenizeCallback>()
            )
        }

        val payPalAccount = slot.captured
        val tokenizePayload = payPalAccount.buildJSON()
        assertEquals(
            "sample-merchant-account-id",
            tokenizePayload.get("merchant_account_id")
        )

        val payPalTokenizePayload = tokenizePayload.getJSONObject("paypalAccount")
        val expectedPayPalTokenizePayload =
            JSONObject().put("correlationId", "sample-client-metadata-id")
                .put("client", JSONObject())
                .put("response", JSONObject().put("webURL", approvalUrl))
                .put("intent", "authorize").put("response_type", "web")
                .put("options", JSONObject().put("validate", false))

        JSONAssert.assertEquals(expectedPayPalTokenizePayload, payPalTokenizePayload, true)
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_whenCancelUriReceived_notifiesCancellationAndSendsAnalyticsEvent() {
        val payPalInternalClient = MockkPayPalInternalClientBuilder().build()

        val approvalUrl = "sample-scheme://onetouch/v1/cancel"

        val browserSwitchResult = mockk<BrowserSwitchFinalResult.Success>()

        every { browserSwitchResult.requestMetadata } returns
            JSONObject().put("client-metadata-id", "sample-client-metadata-id")
                .put("merchant-account-id", "sample-merchant-account-id")
                .put("intent", "authorize").put("approval-url", approvalUrl)
                .put("success-url", "https://example.com/success")
                .put("payment-type", "single-payment")

        val uri = Uri.parse(approvalUrl)
        every { browserSwitchResult.returnUrl } returns uri

        val payPalPaymentAuthResult = PayPalPaymentAuthResult.Success(browserSwitchResult)
        val braintreeClient = MockkBraintreeClientBuilder().build()
        val sut = testPaypalClient(
            braintreeClient,
            payPalInternalClient,
        )

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback)

        val slot = slot<PayPalResult>()
        verify { payPalTokenizeCallback.onPayPalResult(capture(slot)) }

        val result = slot.captured
        assertTrue(result is PayPalResult.Cancel)

        val params = AnalyticsEventParams(
            contextId = null,
            isVaultRequest = false,
            appSwitchUrl = approvalUrl
        )
        verify { braintreeClient.sendAnalyticsEvent(PayPalAnalytics.BROWSER_LOGIN_CANCELED, params, true) }
        verify { analyticsParamRepository.reset() }
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_whenPayPalInternalClientTokenizeResult_callsBackResult() {
        val payPalAccountNonce = mockk<PayPalAccountNonce>(relaxed = true)
        val payPalInternalClient =
            MockkPayPalInternalClientBuilder().tokenizeSuccess(payPalAccountNonce).build()

        val approvalUrl =
            "sample-scheme://onetouch/v1/success?" +
                "PayerID=HERMES-SANDBOX-PAYER-ID" +
                "&paymentId=HERMES-SANDBOX-PAYMENT-ID" +
                "&token=EC-HERMES-SANDBOX-EC-TOKEN"

        val browserSwitchResult = mockk<BrowserSwitchFinalResult.Success>()

        every { browserSwitchResult.requestMetadata } returns
            JSONObject().put("client-metadata-id", "sample-client-metadata-id")
                .put("merchant-account-id", "sample-merchant-account-id")
                .put("intent", "authorize").put("approval-url", approvalUrl)
                .put("success-url", "https://example.com/success")
                .put("payment-type", "single-payment")

        val uri = Uri.parse(approvalUrl)
        every { browserSwitchResult.returnUrl } returns uri

        val payPalPaymentAuthResult = PayPalPaymentAuthResult.Success(browserSwitchResult)
        val braintreeClient = MockkBraintreeClientBuilder().build()
        val sut = testPaypalClient(
            braintreeClient,
            payPalInternalClient,
        )

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback)

        val slot = slot<PayPalResult>()
        verify { payPalTokenizeCallback.onPayPalResult(capture(slot)) }

        val result = slot.captured
        assertTrue(result is PayPalResult.Success)
        assertEquals(payPalAccountNonce, (result as PayPalResult.Success).nonce)

        val params = AnalyticsEventParams(
            contextId = "EC-HERMES-SANDBOX-EC-TOKEN",
            isVaultRequest = false,
            appSwitchUrl = approvalUrl
        )
        verify { braintreeClient.sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_SUCCEEDED, params, true) }
        verify { analyticsParamRepository.reset() }
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_whenCancelUriReceived_sendsAppSwitchCanceledEvent() {
        val payPalInternalClient = MockkPayPalInternalClientBuilder().build()

        val approvalUrl = "https://some-scheme/onetouch/v1/cancel?switch_initiated_time=17166111926211"

        val browserSwitchResult = mockk<BrowserSwitchFinalResult.Success>()

        every { browserSwitchResult.requestMetadata } returns
            JSONObject().put("client-metadata-id", "sample-client-metadata-id")
                .put("merchant-account-id", "sample-merchant-account-id")
                .put("intent", "authorize").put("approval-url", approvalUrl)
                .put("success-url", "https://example.com/success")
                .put("payment-type", "single-payment")

        val uri = Uri.parse(approvalUrl)
        every { browserSwitchResult.returnUrl } returns uri

        val payPalPaymentAuthResult = PayPalPaymentAuthResult.Success(browserSwitchResult)
        val braintreeClient = MockkBraintreeClientBuilder().build()
        val sut = testPaypalClient(
            braintreeClient,
            payPalInternalClient,
        )

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback)

        val slot = slot<PayPalResult>()
        verify { payPalTokenizeCallback.onPayPalResult(capture(slot)) }

        val result = slot.captured
        assertTrue(result is PayPalResult.Cancel)

        val params = AnalyticsEventParams(appSwitchUrl = approvalUrl)
        verify { braintreeClient.sendAnalyticsEvent(PayPalAnalytics.APP_SWITCH_CANCELED, params, true) }
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_whenCancelUriReceived_sendsBrowserLoginCanceledEvent() {
        val payPalInternalClient = MockkPayPalInternalClientBuilder().build()

        val approvalUrl = "https://some-scheme/onetouch/v1/cancel"

        val browserSwitchResult = mockk<BrowserSwitchFinalResult.Success>()

        every { browserSwitchResult.requestMetadata } returns
            JSONObject().put("client-metadata-id", "sample-client-metadata-id")
                .put("merchant-account-id", "sample-merchant-account-id")
                .put("intent", "authorize").put("approval-url", approvalUrl)
                .put("success-url", "https://example.com/success")
                .put("payment-type", "single-payment")

        val uri = Uri.parse(approvalUrl)
        every { browserSwitchResult.returnUrl } returns uri

        val payPalPaymentAuthResult = PayPalPaymentAuthResult.Success(browserSwitchResult)
        val braintreeClient = MockkBraintreeClientBuilder().build()
        val sut = testPaypalClient(
            braintreeClient,
            payPalInternalClient,
        )

        sut.tokenize(payPalPaymentAuthResult, payPalTokenizeCallback)

        val slot = slot<PayPalResult>()
        verify { payPalTokenizeCallback.onPayPalResult(capture(slot)) }

        val result = slot.captured
        assertTrue(result is PayPalResult.Cancel)

        val params = AnalyticsEventParams(appSwitchUrl = approvalUrl)
        verify { braintreeClient.sendAnalyticsEvent(PayPalAnalytics.BROWSER_LOGIN_CANCELED, params, true) }
    }

    private fun testPaypalClient(
        braintreeClient: BraintreeClient,
        payPalInternalClient: PayPalInternalClient,
        dispatcher: CoroutineDispatcher = Dispatchers.Main,
        scope: CoroutineScope = CoroutineScope(dispatcher)
    ): PayPalClient = PayPalClient(
        braintreeClient,
        payPalInternalClient,
        merchantRepository,
        getDefaultAppUseCase,
        getAppLinksCompatibleBrowserUseCase,
        getReturnLinkTypeUseCase,
        getReturnLinkUseCase,
        analyticsParamRepository,
        dispatcher,
        scope
    )
}
