package com.braintreepayments.api.paypal

import android.net.Uri
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.AppSwitchRepository
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.BraintreeRequestCodes
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.Configuration.Companion.fromJson
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.core.GetAppSwitchUseCase
import com.braintreepayments.api.core.GetReturnLinkTypeUseCase
import com.braintreepayments.api.core.GetReturnLinkTypeUseCase.ReturnLinkTypeResult
import com.braintreepayments.api.core.GetReturnLinkUseCase
import com.braintreepayments.api.core.GetReturnLinkUseCase.ReturnLinkResult
import com.braintreepayments.api.core.GetReturnLinkUseCase.ReturnLinkResult.AppLink
import com.braintreepayments.api.core.GetReturnLinkUseCase.ReturnLinkResult.DeepLink
import com.braintreepayments.api.core.LinkType
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import junit.framework.Assert
import junit.framework.TestCase
import org.json.JSONException
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert

@RunWith(RobolectricTestRunner::class)
class PayPalClientUnitTest {
    private var activity = mockk<FragmentActivity>(relaxed = true)
    private val payPalEnabledConfig: Configuration = fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
    private val payPalDisabledConfig: Configuration = fromJson(Fixtures.CONFIGURATION_WITH_DISABLED_PAYPAL)

    private var mockkPayPalTokenizeCallback = mockk<PayPalTokenizeCallback>(relaxed = true)
    private var mockkPaymentAuthCallback: PayPalPaymentAuthCallback = mockk(relaxed = true)

    private var mockkMerchantRepository: MerchantRepository = mockk(relaxed = true)
    private var mockkGetReturnLinkTypeUseCase: GetReturnLinkTypeUseCase =
        mockk<GetReturnLinkTypeUseCase>(relaxed = true)
    private var mockkGetReturnLinkUseCase: GetReturnLinkUseCase = mockk(relaxed = true)
    private val appSwitchRepository: AppSwitchRepository? = null
    private var mockkGetAppSwitchUseCase: GetAppSwitchUseCase = mockk(relaxed = true)
    private var mockkAnalyticsParamRepository: AnalyticsParamRepository = mockk(relaxed = true)

    @Before
    @Throws(JSONException::class)
    fun beforeEach() {
        every { mockkMerchantRepository.returnUrlScheme } returns "com.braintreepayments.demo"
        every { mockkGetReturnLinkUseCase.invoke() } returns AppLink(Uri.parse("www.example.com"))
        every { mockkGetAppSwitchUseCase.invoke() } returns true
        every { mockkGetReturnLinkTypeUseCase.invoke() } returns ReturnLinkTypeResult.APP_LINK
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

        PayPalClient(
            braintreeClient,
            payPalInternalClient,
            mockkMerchantRepository,
            mockkGetReturnLinkTypeUseCase,
            mockkGetReturnLinkUseCase,
            mockkGetAppSwitchUseCase,
            mockkAnalyticsParamRepository
        )

        verify { mockkAnalyticsParamRepository.linkType = LinkType.APP_LINK }
    }

    @Test
    fun initialization_sets_deep_link_in_analyticsParamRepository() {
        every { mockkGetReturnLinkTypeUseCase.invoke() } returns ReturnLinkTypeResult.DEEP_LINK
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

        PayPalClient(
            braintreeClient,
            payPalInternalClient,
            mockkMerchantRepository,
            mockkGetReturnLinkTypeUseCase,
            mockkGetReturnLinkUseCase,
            mockkGetAppSwitchUseCase,
            mockkAnalyticsParamRepository
        )

        verify { mockkAnalyticsParamRepository.linkType = LinkType.DEEP_LINK }
    }

    @OptIn(ExperimentalBetaApi::class)
    @Test
    @Throws(JSONException::class)
    fun createPaymentAuthRequest_callsBackPayPalResponse_sendsStartedAnalytics() {
        val payPalVaultRequest = PayPalVaultRequest(true)
        payPalVaultRequest.merchantAccountId = "sample-merchant-account-id"
        payPalVaultRequest.shopperSessionId = "test-shopper-session-id"

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
            MockkBraintreeClientBuilder().configurationSuccess(payPalEnabledConfig).build()

        val sut = PayPalClient(
            braintreeClient,
            payPalInternalClient,
            mockkMerchantRepository,
            mockkGetReturnLinkTypeUseCase,
            mockkGetReturnLinkUseCase,
            mockkGetAppSwitchUseCase,
            mockkAnalyticsParamRepository
        )
        sut.createPaymentAuthRequest(activity, payPalVaultRequest, mockkPaymentAuthCallback)

        val slot = slot<PayPalPaymentAuthRequest>()
        verify { mockkPaymentAuthCallback.onPayPalPaymentAuthRequest(capture(slot)) }

        val request = slot.captured
        Assert.assertTrue(request is PayPalPaymentAuthRequest.ReadyToLaunch)
        val paymentAuthRequestCaptured =
            (request as PayPalPaymentAuthRequest.ReadyToLaunch).requestParams

        val browserSwitchOptions = paymentAuthRequestCaptured.browserSwitchOptions
        Assert.assertEquals(
            BraintreeRequestCodes.PAYPAL.code,
            browserSwitchOptions!!.requestCode
        )
        TestCase.assertFalse(browserSwitchOptions.isLaunchAsNewTask)

        Assert.assertEquals(
            Uri.parse("https://example.com/approval/url"),
            browserSwitchOptions.url
        )

        val metadata = browserSwitchOptions.metadata
        Assert.assertEquals("https://example.com/approval/url", metadata!!.get("approval-url"))
        Assert.assertEquals("https://example.com/success/url", metadata.get("success-url"))
        Assert.assertEquals("billing-agreement", metadata.get("payment-type"))
        Assert.assertEquals("sample-client-metadata-id", metadata.get("client-metadata-id"))
        Assert.assertEquals("sample-merchant-account-id", metadata.get("merchant-account-id"))
        Assert.assertEquals("paypal-browser", metadata.get("source"))

        verify {
            braintreeClient.sendAnalyticsEvent(
                PayPalAnalytics.TOKENIZATION_STARTED,
                AnalyticsEventParams(shopperSessionId = "test-shopper-session-id"),
                true
            )
        }
    }

    @Test
    fun createPaymentAuthRequest_whenLaunchesBrowserSwitchAsNewTaskEnabled_setsNewTaskOption() {
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

        val sut = PayPalClient(
            braintreeClient,
            payPalInternalClient,
            mockkMerchantRepository,
            mockkGetReturnLinkTypeUseCase,
            mockkGetReturnLinkUseCase,
            mockkGetAppSwitchUseCase,
            mockkAnalyticsParamRepository
        )
        sut.createPaymentAuthRequest(activity, payPalVaultRequest, mockkPaymentAuthCallback)

        val slot = slot<PayPalPaymentAuthRequest>()
        verify { mockkPaymentAuthCallback.onPayPalPaymentAuthRequest(capture(slot)) }

        val request = slot.captured
        Assert.assertTrue(request is PayPalPaymentAuthRequest.ReadyToLaunch)
        Assert.assertTrue((request as PayPalPaymentAuthRequest.ReadyToLaunch).requestParams.browserSwitchOptions?.isLaunchAsNewTask == true)
    }

    @Test
    fun createPaymentAuthRequest_setsAppLinkReturnUrl() {
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

        every { mockkMerchantRepository.appLinkReturnUri } returns Uri.parse("www.example.com")

        val braintreeClient = MockkBraintreeClientBuilder().configurationSuccess(payPalEnabledConfig)
            .build()

        val sut = PayPalClient(
            braintreeClient,
            payPalInternalClient,
            mockkMerchantRepository,
            mockkGetReturnLinkTypeUseCase,
            mockkGetReturnLinkUseCase,
            mockkGetAppSwitchUseCase,
            mockkAnalyticsParamRepository
        )
        sut.createPaymentAuthRequest(activity, payPalVaultRequest, mockkPaymentAuthCallback)

        val slot = slot<PayPalPaymentAuthRequest>()
        verify { mockkPaymentAuthCallback.onPayPalPaymentAuthRequest(capture(slot)) }

        val request = slot.captured
        Assert.assertTrue(request is PayPalPaymentAuthRequest.ReadyToLaunch)
        Assert.assertEquals(
            mockkMerchantRepository.appLinkReturnUri,
            (request as PayPalPaymentAuthRequest.ReadyToLaunch).requestParams.browserSwitchOptions!!.appLinkUri
        )
    }

    @Test
    fun createPaymentAuthRequest_setsDeepLinkReturnUrlScheme() {
        every { mockkGetReturnLinkUseCase.invoke() } returns DeepLink("com.braintreepayments.demo")
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

        val sut = PayPalClient(
            braintreeClient,
            payPalInternalClient,
            mockkMerchantRepository,
            mockkGetReturnLinkTypeUseCase,
            mockkGetReturnLinkUseCase,
            mockkGetAppSwitchUseCase,
            mockkAnalyticsParamRepository
        )
        sut.createPaymentAuthRequest(activity, payPalVaultRequest, mockkPaymentAuthCallback)

        val slot = slot<PayPalPaymentAuthRequest>()
        verify { mockkPaymentAuthCallback.onPayPalPaymentAuthRequest(capture(slot)) }

        val request = slot.captured
        Assert.assertTrue(request is PayPalPaymentAuthRequest.ReadyToLaunch)
        Assert.assertEquals(
            "com.braintreepayments.demo",
            (request as PayPalPaymentAuthRequest.ReadyToLaunch).requestParams.browserSwitchOptions!!.returnUrlScheme
        )
    }

    @Test
    fun createPaymentAuthRequest_returnsAnErrorWhen_getReturnLinkUseCase_returnsAFailure() {
        val exception = BraintreeException()
        every { mockkGetReturnLinkUseCase.invoke() } returns ReturnLinkResult.Failure(exception)

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

        val sut = PayPalClient(
            braintreeClient,
            payPalInternalClient,
            mockkMerchantRepository,
            mockkGetReturnLinkTypeUseCase,
            mockkGetReturnLinkUseCase,
            mockkGetAppSwitchUseCase,
            mockkAnalyticsParamRepository
        )
        sut.createPaymentAuthRequest(activity, payPalVaultRequest, mockkPaymentAuthCallback)

        val slot = slot<PayPalPaymentAuthRequest>()
        verify { mockkPaymentAuthCallback.onPayPalPaymentAuthRequest(capture(slot)) }

        val request = slot.captured
        Assert.assertTrue(request is PayPalPaymentAuthRequest.Failure)
        Assert.assertEquals(exception, (request as PayPalPaymentAuthRequest.Failure).error)
    }

    @Test
    fun createPaymentAuthRequest_whenPayPalNotEnabled_returnsError() {
        val payPalInternalClient = MockkPayPalInternalClientBuilder().build()

        val braintreeClient =
            MockkBraintreeClientBuilder().configurationSuccess(payPalDisabledConfig).build()

        val sut = PayPalClient(
            braintreeClient,
            payPalInternalClient,
            mockkMerchantRepository,
            mockkGetReturnLinkTypeUseCase,
            mockkGetReturnLinkUseCase,
            mockkGetAppSwitchUseCase,
            mockkAnalyticsParamRepository
        )
        sut.createPaymentAuthRequest(
            activity, PayPalCheckoutRequest("1.00", true),
            mockkPaymentAuthCallback
        )

        val slot = slot<PayPalPaymentAuthRequest>()
        verify { mockkPaymentAuthCallback.onPayPalPaymentAuthRequest(capture(slot)) }

        val request = slot.captured
        Assert.assertTrue(request is PayPalPaymentAuthRequest.Failure)
        Assert.assertEquals(
            PayPalClient.Companion.PAYPAL_NOT_ENABLED_MESSAGE,
            (request as PayPalPaymentAuthRequest.Failure).error.message
        )

        val params = AnalyticsEventParams(
            null,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            PayPalClient.Companion.PAYPAL_NOT_ENABLED_MESSAGE
        )
        verify { braintreeClient.sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_FAILED, params, true) }
        verify { mockkAnalyticsParamRepository.reset() }
    }

    @Test
    fun createPaymentAuthRequest_whenCheckoutRequest_whenConfigError_forwardsErrorToListener() {
        val payPalInternalClient = MockkPayPalInternalClientBuilder().build()

        val errorMessage = "Error fetching auth"
        val authError = Exception(errorMessage)
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationError(authError)
            .build()

        val sut = PayPalClient(
            braintreeClient,
            payPalInternalClient,
            mockkMerchantRepository,
            mockkGetReturnLinkTypeUseCase,
            mockkGetReturnLinkUseCase,
            mockkGetAppSwitchUseCase,
            mockkAnalyticsParamRepository
        )
        sut.createPaymentAuthRequest(
            activity,
            PayPalCheckoutRequest("1.00", true),
            mockkPaymentAuthCallback
        )

        val slot = slot<PayPalPaymentAuthRequest>()
        verify { mockkPaymentAuthCallback.onPayPalPaymentAuthRequest(capture(slot)) }

        val request = slot.captured
        Assert.assertTrue(request is PayPalPaymentAuthRequest.Failure)
        Assert.assertEquals(authError, (request as PayPalPaymentAuthRequest.Failure).error)

        val params = AnalyticsEventParams(
            null,
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            errorMessage
        )
        verify { braintreeClient.sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_FAILED, params, true) }
    }

    @Test
    fun requestBillingAgreement_whenConfigError_forwardsErrorToListener() {
        val payPalInternalClient = MockkPayPalInternalClientBuilder().build()

        val errorMessage = "Error fetching auth"
        val authError = Exception(errorMessage)
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationError(authError)
            .build()

        val sut = PayPalClient(
            braintreeClient,
            payPalInternalClient,
            mockkMerchantRepository,
            mockkGetReturnLinkTypeUseCase,
            mockkGetReturnLinkUseCase,
            mockkGetAppSwitchUseCase,
            mockkAnalyticsParamRepository
        )
        sut.createPaymentAuthRequest(activity, PayPalVaultRequest(true), mockkPaymentAuthCallback)

        val slot = slot<PayPalPaymentAuthRequest>()
        verify { mockkPaymentAuthCallback.onPayPalPaymentAuthRequest(capture(slot)) }

        val request = slot.captured
        Assert.assertTrue(request is PayPalPaymentAuthRequest.Failure)
        Assert.assertEquals(authError, (request as PayPalPaymentAuthRequest.Failure).error)

        val params = AnalyticsEventParams(
            null,
            true,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            errorMessage
        )
        verify { braintreeClient.sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_FAILED, params, true) }
    }

    @Test
    fun createPaymentAuthRequest_sets_analyticsParamRepository_didEnablePayPalAppSwitch() {
        val payPalInternalClient = MockkPayPalInternalClientBuilder().build()

        val braintreeClient =
            MockkBraintreeClientBuilder().configurationSuccess(payPalEnabledConfig).build()

        val payPalRequest = PayPalVaultRequest(
            true,
            false,
            null,
            null,
            true
        )

        val sut = PayPalClient(
            braintreeClient,
            payPalInternalClient,
            mockkMerchantRepository,
            mockkGetReturnLinkTypeUseCase,
            mockkGetReturnLinkUseCase,
            mockkGetAppSwitchUseCase,
            mockkAnalyticsParamRepository
        )
        sut.createPaymentAuthRequest(activity, payPalRequest, mockkPaymentAuthCallback)

        verify { mockkAnalyticsParamRepository.didEnablePayPalAppSwitch = true }
    }

    @Test
    fun createPaymentAuthRequest_whenVaultRequest_sendsPayPalRequestViaInternalClient() {
        val payPalInternalClient = MockkPayPalInternalClientBuilder().build()

        val braintreeClient =
            MockkBraintreeClientBuilder().configurationSuccess(payPalEnabledConfig).build()

        val payPalRequest = PayPalVaultRequest(true)

        val sut = PayPalClient(
            braintreeClient,
            payPalInternalClient,
            mockkMerchantRepository,
            mockkGetReturnLinkTypeUseCase,
            mockkGetReturnLinkUseCase,
            mockkGetAppSwitchUseCase,
            mockkAnalyticsParamRepository
        )
        sut.createPaymentAuthRequest(activity, payPalRequest, mockkPaymentAuthCallback)

        verify {
            payPalInternalClient.sendRequest(
                activity,
                payPalRequest,
                any<PayPalInternalClientCallback>()
            )
        }
    }

    @Test
    fun createPaymentAuthRequest_whenCheckoutRequest_sendsPayPalRequestViaInternalClient() {
        val payPalInternalClient = MockkPayPalInternalClientBuilder().build()

        val braintreeClient =
            MockkBraintreeClientBuilder().configurationSuccess(payPalEnabledConfig).build()

        val payPalRequest = PayPalCheckoutRequest("1.00", true)

        val sut = PayPalClient(
            braintreeClient,
            payPalInternalClient,
            mockkMerchantRepository,
            mockkGetReturnLinkTypeUseCase,
            mockkGetReturnLinkUseCase,
            mockkGetAppSwitchUseCase,
            mockkAnalyticsParamRepository
        )
        sut.createPaymentAuthRequest(activity, payPalRequest, mockkPaymentAuthCallback)

        verify {
            payPalInternalClient.sendRequest(
                activity,
                payPalRequest,
                any<PayPalInternalClientCallback>()
            )
        }
    }

    @Test
    fun createPaymentAuthRequest_whenVaultRequest_sendsAppSwitchStartedEvent() {
        val payPalVaultRequest = PayPalVaultRequest(true).apply {
            userAuthenticationEmail = "some@email.com"
            enablePayPalAppSwitch = true
            merchantAccountId = "sample-merchant-account-id"
        }

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

        every { payPalInternalClient.isPayPalInstalled(activity) } returns true
        every { payPalInternalClient.isAppSwitchEnabled(payPalVaultRequest) } returns true

        val braintreeClient =
            MockkBraintreeClientBuilder().configurationSuccess(payPalEnabledConfig).build()

        val sut = PayPalClient(
            braintreeClient,
            payPalInternalClient,
            mockkMerchantRepository,
            mockkGetReturnLinkTypeUseCase,
            mockkGetReturnLinkUseCase,
            mockkGetAppSwitchUseCase,
            mockkAnalyticsParamRepository
        )
        sut.createPaymentAuthRequest(activity, payPalVaultRequest, mockkPaymentAuthCallback)

        val slot = slot<PayPalPaymentAuthRequest>()
        verify { mockkPaymentAuthCallback.onPayPalPaymentAuthRequest(capture(slot)) }

        val request = slot.captured
        Assert.assertTrue(request is PayPalPaymentAuthRequest.ReadyToLaunch)
        val paymentAuthRequestCaptured =
            (request as PayPalPaymentAuthRequest.ReadyToLaunch).requestParams

        val browserSwitchOptions = paymentAuthRequestCaptured.browserSwitchOptions
        Assert.assertEquals(
            BraintreeRequestCodes.PAYPAL.code,
            browserSwitchOptions!!.requestCode
        )
        TestCase.assertFalse(browserSwitchOptions.isLaunchAsNewTask)

        val params = AnalyticsEventParams(
            null,
            true
        )
        verify { braintreeClient.sendAnalyticsEvent(PayPalAnalytics.APP_SWITCH_STARTED, params, true) }
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_withBillingAgreement_tokenizesResponseOnSuccess() {
        val payPalInternalClient = MockkPayPalInternalClientBuilder().build()
        val approvalUrl =
            "sample-scheme://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&ba_token=EC-HERMES-SANDBOX-EC-TOKEN"
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
        val sut = PayPalClient(
            braintreeClient,
            payPalInternalClient,
            mockkMerchantRepository,
            mockkGetReturnLinkTypeUseCase,
            mockkGetReturnLinkUseCase,
            mockkGetAppSwitchUseCase,
            mockkAnalyticsParamRepository
        )

        sut.tokenize(payPalPaymentAuthResult, mockkPayPalTokenizeCallback)

        val slot = slot<PayPalAccount>()
        verify {
            payPalInternalClient.tokenize(
                capture(slot),
                any<PayPalInternalTokenizeCallback>()
            )
        }

        val payPalAccount = slot.captured
        val tokenizePayload = payPalAccount.buildJSON()
        Assert.assertEquals(
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
            "sample-scheme://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN"

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
        val sut = PayPalClient(
            braintreeClient,
            payPalInternalClient,
            mockkMerchantRepository,
            mockkGetReturnLinkTypeUseCase,
            mockkGetReturnLinkUseCase,
            mockkGetAppSwitchUseCase,
            mockkAnalyticsParamRepository
        )

        sut.tokenize(payPalPaymentAuthResult, mockkPayPalTokenizeCallback)

        val slot = slot<PayPalAccount>()
        verify {
            payPalInternalClient.tokenize(
                capture(slot),
                any<PayPalInternalTokenizeCallback>()
            )
        }

        val payPalAccount = slot.captured
        val tokenizePayload = payPalAccount.buildJSON()
        Assert.assertEquals(
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
        val sut = PayPalClient(
            braintreeClient,
            payPalInternalClient,
            mockkMerchantRepository,
            mockkGetReturnLinkTypeUseCase,
            mockkGetReturnLinkUseCase,
            mockkGetAppSwitchUseCase,
            mockkAnalyticsParamRepository
        )

        sut.tokenize(payPalPaymentAuthResult, mockkPayPalTokenizeCallback)

        val slot = slot<PayPalResult>()
        verify { mockkPayPalTokenizeCallback.onPayPalResult(capture(slot)) }

        val result = slot.captured
        Assert.assertTrue(result is PayPalResult.Cancel)

        val params = AnalyticsEventParams(null, false)
        verify { braintreeClient.sendAnalyticsEvent(PayPalAnalytics.BROWSER_LOGIN_CANCELED, params, true) }
        verify { mockkAnalyticsParamRepository.reset() }
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_whenPayPalInternalClientTokenizeResult_callsBackResult() {
        val payPalAccountNonce = mockk<PayPalAccountNonce>(relaxed = true)
        val payPalInternalClient =
            MockkPayPalInternalClientBuilder().tokenizeSuccess(payPalAccountNonce).build()

        val approvalUrl =
            "sample-scheme://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN"

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
        val sut = PayPalClient(
            braintreeClient,
            payPalInternalClient,
            mockkMerchantRepository,
            mockkGetReturnLinkTypeUseCase,
            mockkGetReturnLinkUseCase,
            mockkGetAppSwitchUseCase,
            mockkAnalyticsParamRepository
        )

        sut.tokenize(payPalPaymentAuthResult, mockkPayPalTokenizeCallback)

        val slot = slot<PayPalResult>()
        verify { mockkPayPalTokenizeCallback.onPayPalResult(capture(slot)) }

        val result = slot.captured
        Assert.assertTrue(result is PayPalResult.Success)
        Assert.assertEquals(payPalAccountNonce, (result as PayPalResult.Success).nonce)

        val params = AnalyticsEventParams(
            "EC-HERMES-SANDBOX-EC-TOKEN",
            false
        )
        verify { braintreeClient.sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_SUCCEEDED, params, true) }
        verify { mockkAnalyticsParamRepository.reset() }
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_whenPayPalInternalClientTokenizeResult_sendsAppSwitchSucceededEvents() {
        val payPalAccountNonce = mockk<PayPalAccountNonce>(relaxed = true)
        val payPalInternalClient =
            MockkPayPalInternalClientBuilder().tokenizeSuccess(payPalAccountNonce).build()

        val approvalUrl =
            "sample-scheme://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN&switch_initiated_time=17166111926211"

        val browserSwitchResult = mockk<BrowserSwitchFinalResult.Success>(relaxed = true)

        every { browserSwitchResult.requestMetadata } returns JSONObject().also {
            it.put("client-metadata-id", "sample-client-metadata-id")
            it.put("merchant-account-id", "sample-merchant-account-id")
            it.put("intent", "authorize").put("approval-url", approvalUrl)
            it.put("success-url", "https://example.com/success")
            it.put("payment-type", "single-payment")
        }

        val uri = Uri.parse(approvalUrl)
        every { browserSwitchResult.returnUrl } returns uri

        val payPalPaymentAuthResult = PayPalPaymentAuthResult.Success(browserSwitchResult)
        val braintreeClient = MockkBraintreeClientBuilder().build()
        val sut = PayPalClient(
            braintreeClient,
            payPalInternalClient,
            mockkMerchantRepository,
            mockkGetReturnLinkTypeUseCase,
            mockkGetReturnLinkUseCase,
            mockkGetAppSwitchUseCase,
            mockkAnalyticsParamRepository
        )

        sut.tokenize(payPalPaymentAuthResult, mockkPayPalTokenizeCallback)

        val slot = slot<PayPalResult>()
        verify { mockkPayPalTokenizeCallback.onPayPalResult(capture(slot)) }

        val result = slot.captured
        Assert.assertTrue(result is PayPalResult.Success)
        Assert.assertEquals(payPalAccountNonce, (result as PayPalResult.Success).nonce)

        val params = AnalyticsEventParams("EC-HERMES-SANDBOX-EC-TOKEN")
        verify { braintreeClient.sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_SUCCEEDED, params, true) }
        val appSwitchParams = AnalyticsEventParams(
            payPalContextId = "EC-HERMES-SANDBOX-EC-TOKEN",
            isVaultRequest = false,
            appSwitchUrl = "sample-scheme://onetouch/v1/success?PayerID=HERMES-SANDBOX-PAYER-ID&paymentId=HERMES-SANDBOX-PAYMENT-ID&token=EC-HERMES-SANDBOX-EC-TOKEN&switch_initiated_time=17166111926211"
        )
        verify { braintreeClient.sendAnalyticsEvent(PayPalAnalytics.APP_SWITCH_SUCCEEDED, appSwitchParams, true) }
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_whenPayPalNotEnabled_sendsAppSwitchFailedEvents() {
        val payPalInternalClient = MockkPayPalInternalClientBuilder().build()
        val approvalUrl =
            "https://some-scheme/onetouch/v1/cancel?switch_initiated_time=17166111926211"
        val browserSwitchResult = mockk<BrowserSwitchFinalResult.Success>()

        every { browserSwitchResult.requestMetadata } returns
            JSONObject().put("client-metadata-id", "sample-client-metadata-id")
                .put("merchant-account-id", "sample-merchant-account-id")
                .put("intent", "authorize").put(
                    "approval-url",
                    "https://some-scheme/onetouch/v1/cancel?token=SOME-BA&switch_initiated_time=17166111926211"
                )
                .put("success-url", "https://example.com/cancel")
                .put("payment-type", "single-payment")

        val uri = Uri.parse(approvalUrl)
        every { browserSwitchResult.returnUrl } returns uri

        val payPalPaymentAuthResult = PayPalPaymentAuthResult.Success(browserSwitchResult)
        val braintreeClient = MockkBraintreeClientBuilder().build()
        val sut = PayPalClient(
            braintreeClient,
            payPalInternalClient,
            mockkMerchantRepository,
            mockkGetReturnLinkTypeUseCase,
            mockkGetReturnLinkUseCase,
            mockkGetAppSwitchUseCase,
            mockkAnalyticsParamRepository
        )

        sut.tokenize(payPalPaymentAuthResult, mockkPayPalTokenizeCallback)

        val params = AnalyticsEventParams(
            "SOME-BA",
            false,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            PayPalClient.Companion.BROWSER_SWITCH_EXCEPTION_MESSAGE
        )
        verify { braintreeClient.sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_FAILED, params, true) }
        val appSwitchParams = AnalyticsEventParams(
            "SOME-BA",
            false,
            null,
            null,
            null,
            null,
            "https://some-scheme/onetouch/v1/cancel?token=SOME-BA&switch_initiated_time=17166111926211",
            null,
            null,
            null,
            null,
            PayPalClient.Companion.BROWSER_SWITCH_EXCEPTION_MESSAGE
        )
        verify { braintreeClient.sendAnalyticsEvent(PayPalAnalytics.APP_SWITCH_FAILED, appSwitchParams, true) }
        verify { mockkAnalyticsParamRepository.reset() }
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_whenCancelUriReceived_sendsAppSwitchCanceledEvents() {
        val payPalInternalClient = MockkPayPalInternalClientBuilder().build()

        val approvalUrl =
            "https://some-scheme/onetouch/v1/cancel?switch_initiated_time=17166111926211"

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
        val sut = PayPalClient(
            braintreeClient,
            payPalInternalClient,
            mockkMerchantRepository,
            mockkGetReturnLinkTypeUseCase,
            mockkGetReturnLinkUseCase,
            mockkGetAppSwitchUseCase,
            mockkAnalyticsParamRepository
        )

        sut.tokenize(payPalPaymentAuthResult, mockkPayPalTokenizeCallback)

        val slot = slot<PayPalResult>()
        verify { mockkPayPalTokenizeCallback.onPayPalResult(capture(slot)) }

        val result = slot.captured
        Assert.assertTrue(result is PayPalResult.Cancel)

        val params = AnalyticsEventParams()
        verify { braintreeClient.sendAnalyticsEvent(PayPalAnalytics.BROWSER_LOGIN_CANCELED, params, true) }
        verify { braintreeClient.sendAnalyticsEvent(PayPalAnalytics.APP_SWITCH_CANCELED, params, true) }
    }
}
