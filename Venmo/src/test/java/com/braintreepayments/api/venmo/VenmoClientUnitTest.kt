package com.braintreepayments.api.venmo

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.ApiClient
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.BraintreeRequestCodes
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.GetReturnLinkTypeUseCase
import com.braintreepayments.api.core.GetReturnLinkUseCase
import com.braintreepayments.api.core.IntegrationType
import com.braintreepayments.api.core.LinkType
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import org.json.JSONObject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertFalse
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class VenmoClientUnitTest {

    private lateinit var sut: VenmoClient
    private lateinit var apiClient: ApiClient
    private lateinit var venmoApi: VenmoApi
    private lateinit var sharedPrefsWriter: VenmoSharedPrefsWriter
    private lateinit var analyticsParamRepository: AnalyticsParamRepository
    private lateinit var getReturnLinkTypeUseCase: GetReturnLinkTypeUseCase
    private lateinit var context: Context
    private lateinit var clientToken: Authorization
    private lateinit var tokenizationKey: Authorization
    private lateinit var browserSwitchResult: BrowserSwitchFinalResult.Success

    private lateinit var venmoTokenizeCallback: VenmoTokenizeCallback
    private lateinit var venmoPaymentAuthRequestCallback: VenmoPaymentAuthRequestCallback
    private lateinit var paymentAuthResult: VenmoPaymentAuthResult.Success

    private var merchantRepository = mockk<MerchantRepository>(relaxed = true)
    private var venmoRepository= mockk<VenmoRepository>(relaxed = true)
    private var getReturnLinkUseCase = mockk<GetReturnLinkUseCase>(relaxed = true)

    private val venmoEnabledConfiguration: Configuration =
        Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO)
    private val venmoDisabledConfiguration: Configuration =
        Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
    private val SUCCESS_URL: Uri =
        Uri.parse("sample-scheme://x-callback-url/vzero/auth/venmo/success?resource_id=a-resource-id")
    private val SUCCESS_URL_WITHOUT_RESOURCE_ID: Uri =
        Uri.parse(
            "sample-scheme://x-callback-url/vzero/auth/venmo/success?username=venmojoe&payment_method_nonce=fakenonce"
        )
    private val CANCEL_URL: Uri = Uri.parse("sample-scheme://x-callback-url/vzero/auth/venmo/cancel")
    private val appSwitchUrl: Uri = Uri.parse("https://example.com")

    private val expectedAnalyticsParams: AnalyticsEventParams  = AnalyticsEventParams(
        null, false, null, null, null, null,
        appSwitchUrl.toString()
    )

    private val expectedVaultAnalyticsParams: AnalyticsEventParams  = AnalyticsEventParams(
        null, true, null, null, null, null,
        appSwitchUrl.toString()
    )

    @Before
    fun beforeEach(){
        apiClient = mockk(relaxed = true)
        venmoApi = mockk(relaxed = true)
        analyticsParamRepository = mockk(relaxed = true)
        getReturnLinkTypeUseCase = mockk(relaxed = true)
        context = ApplicationProvider.getApplicationContext()


        venmoTokenizeCallback = mockk(relaxed = true)
        venmoPaymentAuthRequestCallback = mockk(relaxed = true)
        sharedPrefsWriter = mockk(relaxed = true)

        clientToken = Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN)
        tokenizationKey = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        browserSwitchResult = mockk(relaxed = true)
        paymentAuthResult = VenmoPaymentAuthResult.Success(browserSwitchResult)
        // Why are static looking values in the before?

        every { analyticsParamRepository.sessionId } returns "session-id"
        every { merchantRepository.integrationType } returns IntegrationType.CUSTOM
        every { merchantRepository.applicationContext } returns context
        every { venmoRepository.venmoUrl } returns appSwitchUrl
        every { getReturnLinkUseCase.invoke() } returns GetReturnLinkUseCase.ReturnLinkResult.AppLink(appSwitchUrl)
        every { getReturnLinkTypeUseCase.invoke() } returns GetReturnLinkTypeUseCase.ReturnLinkTypeResult.APP_LINK
    }

    @Test
    fun initialization_sets_app_link_in_analyticsParamRepository() {
        every { getReturnLinkTypeUseCase.invoke() } returns GetReturnLinkTypeUseCase.ReturnLinkTypeResult.APP_LINK
        var braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(venmoEnabledConfiguration)
            .build()

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )

        verify { analyticsParamRepository.linkType = LinkType.APP_LINK }
    }

    @Test
    fun initialization_sets_deep_link_in_analyticsParamRepository() {
        every { getReturnLinkTypeUseCase.invoke() } returns GetReturnLinkTypeUseCase.ReturnLinkTypeResult.DEEP_LINK
        var braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(venmoEnabledConfiguration)
            .build()

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )

        verify { analyticsParamRepository.linkType = LinkType.DEEP_LINK }
    }

    @Test
    fun createPaymentAuthRequest_whenCreatePaymentContextFails_collectAddressWithEcdDisabled() {
        var errorDesc = "Cannot collect customer data when ECD is disabled. Enable this feature " +
                "in the Control Panel to collect this data."

        var braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(venmoEnabledConfiguration)
            .build()

        every { merchantRepository.authorization } returns clientToken

        venmoApi = MockkVenmoApiBuilder()
            .createPaymentContextSuccess("venmo-payment-context-id")
            .build()

        var authRequestSlot = slot<VenmoPaymentAuthRequest>()
        var analyticsSlot = slot<AnalyticsEventParams>()

        var request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE,
            profileId = "sample-venmo-merchant",
            collectCustomerBillingAddress = true
        )

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback)

        verify {
            venmoPaymentAuthRequestCallback.onVenmoPaymentAuthRequest(
                capture(authRequestSlot)
            )
        }

        verify {
            braintreeClient.sendAnalyticsEvent(
                VenmoAnalytics.TOKENIZE_FAILED,
                capture(analyticsSlot),
                true
            )
        }
        assertFalse { analyticsSlot.captured.isVaultRequest }
        assertEquals(expectedAnalyticsParams.appSwitchUrl, analyticsSlot.captured.appSwitchUrl)
        assertEquals(errorDesc, analyticsSlot.captured.errorDescription)
        verify { analyticsParamRepository.reset() }
        assertTrue { authRequestSlot.captured is VenmoPaymentAuthRequest.Failure }
        assertEquals(
            errorDesc,
            (authRequestSlot.captured as VenmoPaymentAuthRequest.Failure).error.message
        )
    }

    @Test
    fun createPaymentAuthRequest_withDeepLink_whenCreatePaymentContextSucceeds_createsVenmoAuthChallenge() {
        var braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(venmoEnabledConfiguration)
            .returnUrlScheme("com.example")
            .build()

        every { merchantRepository.authorization } returns clientToken
        every { getReturnLinkUseCase.invoke() } returns GetReturnLinkUseCase.ReturnLinkResult.DeepLink("com.example")

        venmoApi = MockkVenmoApiBuilder()
            .createPaymentContextSuccess("venmo-payment-context-id")
            .build()

        var request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE,
            profileId = "sample-venmo-merchant",
            shouldVault = false
        )

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback)

        var authRequestSlot = slot<VenmoPaymentAuthRequest>()
        verifyOrder {
            braintreeClient.sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_STARTED, AnalyticsEventParams(), true)
            venmoPaymentAuthRequestCallback.onVenmoPaymentAuthRequest(capture(authRequestSlot))
        }

        var paymentAuthRequest = authRequestSlot.captured
        assertTrue { paymentAuthRequest is VenmoPaymentAuthRequest.ReadyToLaunch }

        var params = (paymentAuthRequest as VenmoPaymentAuthRequest.ReadyToLaunch).requestParams
        assertEquals(BraintreeRequestCodes.VENMO.code, params.browserSwitchOptions.requestCode)
        assertEquals("com.example", params.browserSwitchOptions.returnUrlScheme)

        var url = params.browserSwitchOptions.url
        assertEquals("com.example://x-callback-url/vzero/auth/venmo/success", url!!.getQueryParameter("x-success"))
        assertEquals("com.example://x-callback-url/vzero/auth/venmo/error", url.getQueryParameter("x-error"))
        assertEquals("com.example://x-callback-url/vzero/auth/venmo/cancel", url.getQueryParameter("x-cancel"))
        assertEquals("sample-venmo-merchant", url.getQueryParameter("braintree_merchant_id"))
        assertEquals("venmo-payment-context-id", url.getQueryParameter("resource_id"))
        assertEquals("MOBILE_APP", url.getQueryParameter("customerClient"))

        var metadata = url.getQueryParameter("braintree_sdk_data")
        var metadataString = String(Base64.decode(metadata, Base64.DEFAULT))
        var expectedMetadata = String.format(
            "{\"_meta\":" +
                    "{\"platform\":\"android\"," +
                    "\"sessionId\":\"session-id\"," +
                    "\"integration\":\"custom\"," +
                    "\"version\":\"%s\"}}",
            com.braintreepayments.api.core.BuildConfig.VERSION_NAME)
        assertEquals(expectedMetadata, metadataString)
    }

    @Test
    fun createPaymentAuthRequest_whenConfigurationException_forwardsExceptionToListener() {
        var braintreeClient = MockkBraintreeClientBuilder()
            .configurationError(Exception("Configuration fetching error"))
            .build()

        var request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE,
            profileId = null,
            shouldVault = false
        )

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback)

        var authRequestSlot = slot<VenmoPaymentAuthRequest>()
        verify{ venmoPaymentAuthRequestCallback.onVenmoPaymentAuthRequest(capture(authRequestSlot)) }
        assertTrue { authRequestSlot.captured is VenmoPaymentAuthRequest.Failure }
        assertEquals("Configuration fetching error",
            (authRequestSlot.captured as VenmoPaymentAuthRequest.Failure).error.message)

        var analyticsSlot = slot<AnalyticsEventParams>()
        verify {
            braintreeClient.sendAnalyticsEvent(
                VenmoAnalytics.TOKENIZE_FAILED,
                capture(analyticsSlot),
                true
            )
        }
        assertFalse { analyticsSlot.captured.isVaultRequest }
        assertEquals(expectedAnalyticsParams.appSwitchUrl, analyticsSlot.captured.appSwitchUrl)
        assertEquals("Configuration fetching error", analyticsSlot.captured.errorDescription)
    }

    @Test
    fun createPaymentAuthRequest_whenVenmoNotEnabled_forwardsExceptionToListener() {
        var braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(venmoDisabledConfiguration)
            .build()

        var request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE,
            profileId = null,
            shouldVault = false
        )

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback)

        var authRequestSlot = slot<VenmoPaymentAuthRequest>()
        verify{ venmoPaymentAuthRequestCallback.onVenmoPaymentAuthRequest(capture(authRequestSlot)) }
        assertTrue(authRequestSlot.captured is VenmoPaymentAuthRequest.Failure)
        assertEquals(
            "Venmo is not enabled",
            (authRequestSlot.captured as VenmoPaymentAuthRequest.Failure).error.message
        )

        var analyticsSlot = slot<AnalyticsEventParams>()
        verify {
            braintreeClient.sendAnalyticsEvent(
                VenmoAnalytics.TOKENIZE_FAILED,
                capture(analyticsSlot),
                true
            )
        }
        assertFalse { analyticsSlot.captured.isVaultRequest }
        assertEquals(expectedAnalyticsParams.appSwitchUrl, analyticsSlot.captured.appSwitchUrl)
        assertEquals("Venmo is not enabled", analyticsSlot.captured.errorDescription)
    }

    @Test
    fun createPaymentAuthRequest_whenProfileIdIsNull_appSwitchesWithMerchantId() {
        var braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(venmoEnabledConfiguration)
            .build()

        every { merchantRepository.authorization } returns clientToken

        venmoApi = MockkVenmoApiBuilder()
            .createPaymentContextSuccess("venmo-payment-context-id")
            .build()

        var request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE,
            profileId = null,
            shouldVault = false
        )

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback)

        var authRequestSlot = slot<VenmoPaymentAuthRequest>()
        verify{ venmoPaymentAuthRequestCallback.onVenmoPaymentAuthRequest(capture(authRequestSlot)) }
        var paymentAuthRequest = authRequestSlot.captured
        assertTrue { paymentAuthRequest is VenmoPaymentAuthRequest.ReadyToLaunch }
        var params = (paymentAuthRequest as VenmoPaymentAuthRequest.ReadyToLaunch).requestParams
        var url = params.browserSwitchOptions.url
        assertEquals("merchant-id", url!!.getQueryParameter("braintree_merchant_id"))
    }

    @Test
    fun createPaymentAuthRequest_whenAppLinkUriSet_appSwitchesWithAppLink() {
        var braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(venmoEnabledConfiguration)
            .build()

        venmoApi = MockkVenmoApiBuilder()
            .createPaymentContextSuccess("venmo-payment-context-id")
            .build()

        var request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE,
            profileId = null,
            shouldVault = false
        )

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback)

        var authRequestSlot = slot<VenmoPaymentAuthRequest>()
        verify{ venmoPaymentAuthRequestCallback.onVenmoPaymentAuthRequest(capture(authRequestSlot)) }
        var paymentAuthRequest = authRequestSlot.captured
        assertTrue { paymentAuthRequest is VenmoPaymentAuthRequest.ReadyToLaunch }
        var params = (paymentAuthRequest as VenmoPaymentAuthRequest.ReadyToLaunch).requestParams
        var url = params.browserSwitchOptions.url
        assertEquals("https://example.com/success", url!!.getQueryParameter("x-success"));
        assertEquals("https://example.com/error", url.getQueryParameter("x-error"));
        assertEquals("https://example.com/cancel", url.getQueryParameter("x-cancel"));
    }

    @Test
    fun createPaymentAuthRequest_throws_error_when_getReturnLinkUseCase_returnsFailure() {
        var exception = BraintreeException()
        var braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(venmoEnabledConfiguration)
            .build()

        every { merchantRepository.authorization } returns clientToken
        every { getReturnLinkUseCase.invoke() } returns
                GetReturnLinkUseCase.ReturnLinkResult.Failure(exception)

        venmoApi = MockkVenmoApiBuilder()
            .createPaymentContextSuccess("venmo-payment-context-id")
            .build()

        var request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE,
            profileId = "second-pwv-profile-id",
            shouldVault = false
        )

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback)

        var authRequestSlot = slot<VenmoPaymentAuthRequest>()
        verify{ venmoPaymentAuthRequestCallback.onVenmoPaymentAuthRequest(capture(authRequestSlot)) }
        assertTrue(authRequestSlot.captured is VenmoPaymentAuthRequest.Failure)
        assertEquals(exception, (authRequestSlot.captured as VenmoPaymentAuthRequest.Failure).error)
    }

    @Test
    fun createPaymentAuthRequest_whenProfileIdIsSpecified_appSwitchesWithProfileIdAndAccessToken() {
        var braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(venmoEnabledConfiguration)
            .build()

        every { merchantRepository.authorization } returns clientToken

        venmoApi = MockkVenmoApiBuilder()
            .createPaymentContextSuccess("venmo-payment-context-id")
            .build()

        var request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE,
            profileId = "second-pwv-profile-id",
            shouldVault = false
        )

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback)

        var authRequestSlot = slot<VenmoPaymentAuthRequest>()
        verify{ venmoPaymentAuthRequestCallback.onVenmoPaymentAuthRequest(capture(authRequestSlot)) }
        var paymentAuthRequest = authRequestSlot.captured
        assertTrue { paymentAuthRequest is VenmoPaymentAuthRequest.ReadyToLaunch }
        var params = (paymentAuthRequest as VenmoPaymentAuthRequest.ReadyToLaunch).requestParams
        var url = params.browserSwitchOptions.url
        assertEquals("second-pwv-profile-id", url!!.getQueryParameter("braintree_merchant_id"))
        assertEquals("venmo-payment-context-id", url.getQueryParameter("resource_id"))
    }

    @Test
    fun createPaymentAuthRequest_sendsAnalyticsEvent() {
        var braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(venmoDisabledConfiguration)
            .build()

        var request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE,
            profileId = null,
            shouldVault = false
        )

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback)

        verify {
            braintreeClient.sendAnalyticsEvent(
                VenmoAnalytics.TOKENIZE_STARTED,
                AnalyticsEventParams(),
                true
            )
        }
    }

    @Test
    fun createPaymentAuthRequest_whenShouldVaultIsTrue_persistsVenmoVaultTrue() {
        var braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(venmoEnabledConfiguration)
            .build()

        every { merchantRepository.authorization } returns clientToken

        venmoApi = MockkVenmoApiBuilder()
            .createPaymentContextSuccess("venmo-payment-context-id")
            .build()

        var request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE,
            profileId = null,
            shouldVault = true
        )

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback)

        verify { sharedPrefsWriter.persistVenmoVaultOption(context, true) }
    }

    @Test
    fun createPaymentAuthRequest_whenShouldVaultIsFalse_persistsVenmoVaultFalse() {
        var braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(venmoEnabledConfiguration)
            .build()

        every { merchantRepository.authorization } returns clientToken

        venmoApi = MockkVenmoApiBuilder()
            .createPaymentContextSuccess("venmo-payment-context-id")
            .build()

        var request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE,
            profileId = null,
            shouldVault = false
        )

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback)

        verify { sharedPrefsWriter.persistVenmoVaultOption(context, false) }
    }

    @Test
    fun createPaymentAuthRequest_whenVenmoApiError_forwardsErrorToListener_andSendsAnalytics() {
        var graphQLError =  BraintreeException("GraphQL error")
        var braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(venmoEnabledConfiguration)
            .sendGraphQLPOSTErrorResponse(graphQLError)
            .build()

        venmoApi = MockkVenmoApiBuilder()
            .createPaymentContextError(graphQLError)
            .build()

        var request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE,
            shouldVault = true
        )

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.createPaymentAuthRequest(context, request, venmoPaymentAuthRequestCallback)

        var authRequestSlot = slot<VenmoPaymentAuthRequest>()
        verify{ venmoPaymentAuthRequestCallback.onVenmoPaymentAuthRequest(capture(authRequestSlot)) }
        assertTrue(authRequestSlot.captured is VenmoPaymentAuthRequest.Failure)
        assertEquals(graphQLError, (authRequestSlot.captured as VenmoPaymentAuthRequest.Failure).error)

        var analyticsSlot = slot<AnalyticsEventParams>()
        verify {
            braintreeClient.sendAnalyticsEvent(
                VenmoAnalytics.TOKENIZE_FAILED,
                capture(analyticsSlot),
                true
            )
        }
        assertFalse { analyticsSlot.captured.isVaultRequest }
        assertEquals(expectedAnalyticsParams.appSwitchUrl, analyticsSlot.captured.appSwitchUrl)
        assertEquals(graphQLError.message, analyticsSlot.captured.errorDescription)
    }

    @Test
    fun tokenize_withPaymentContextId_requestFromVenmoApi() {
        var braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(venmoEnabledConfiguration)
            .build()

        every { browserSwitchResult.returnUrl } returns SUCCESS_URL
        every { merchantRepository.authorization } returns clientToken

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.tokenize(paymentAuthResult, venmoTokenizeCallback)

        verify {
            venmoApi.createNonceFromPaymentContext(
                "a-resource-id",
                any<VenmoInternalCallback>()
            )
        }

        verify {
            braintreeClient.sendAnalyticsEvent(
                VenmoAnalytics.APP_SWITCH_SUCCEEDED,
                expectedAnalyticsParams,
                true
            )
        }
    }

    @Test
    fun tokenize_withPaymentAuthResult_whenUserCanceled_returnsCancelAndSendsAnalytics() {
        var braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(venmoEnabledConfiguration)
            .build()

        every { browserSwitchResult.returnUrl } returns CANCEL_URL
        every { merchantRepository.authorization } returns clientToken

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.tokenize(paymentAuthResult, venmoTokenizeCallback)

        var resultSlot = slot<VenmoResult>()
        verify { venmoTokenizeCallback.onVenmoResult(capture(resultSlot)) }
        assertTrue(resultSlot.captured is VenmoResult.Cancel)
        verify {
            braintreeClient.sendAnalyticsEvent(
                VenmoAnalytics.APP_SWITCH_CANCELED,
                expectedAnalyticsParams,
                true
            )
        }
        verify { analyticsParamRepository.reset() }
    }

    @Test
    fun tokenize_onGraphQLPostSuccess_returnsNonceToListener_andSendsAnalytics() {
        var braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(venmoEnabledConfiguration)
            .build()

        every { browserSwitchResult.returnUrl } returns SUCCESS_URL
        every { merchantRepository.authorization } returns clientToken

        venmoApi = MockkVenmoApiBuilder()
            .createNonceFromPaymentContextSuccess(VenmoAccountNonce.fromJSON(
                JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE)))
            .build()

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.tokenize(paymentAuthResult, venmoTokenizeCallback)

        var resultSlot = slot<VenmoResult>()
        verify { venmoTokenizeCallback.onVenmoResult(capture(resultSlot)) }
        assertTrue(resultSlot.captured is VenmoResult.Success)
        var nonce = (resultSlot.captured as VenmoResult.Success).nonce
        assertEquals("fake-venmo-nonce", nonce.string)
        assertEquals("venmojoe", nonce.username)

        verify {
            braintreeClient.sendAnalyticsEvent(
                VenmoAnalytics.TOKENIZE_SUCCEEDED,
                expectedAnalyticsParams,
                true
            )
        }
        verify { analyticsParamRepository.reset() }
    }

    @Test
    fun tokenize_onGraphQLPostFailure_forwardsExceptionToListener_andSendsAnalytics() {
        var graphQLError =  BraintreeException("GraphQL error")
        var braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(venmoEnabledConfiguration)
            .sendGraphQLPOSTErrorResponse(graphQLError)
            .build()

        every { browserSwitchResult.returnUrl } returns SUCCESS_URL
        every { merchantRepository.authorization } returns clientToken

        venmoApi = MockkVenmoApiBuilder()
            .createNonceFromPaymentContextError(graphQLError)
            .build()

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.tokenize(paymentAuthResult, venmoTokenizeCallback)

        var resultSlot = slot<VenmoResult>()
        verify { venmoTokenizeCallback.onVenmoResult(capture(resultSlot)) }
        assertTrue(resultSlot.captured is VenmoResult.Failure)
        assertEquals(graphQLError, (resultSlot.captured as VenmoResult.Failure).error)

        var analyticsSlot = slot<AnalyticsEventParams>()
        verify {
            braintreeClient.sendAnalyticsEvent(
                VenmoAnalytics.TOKENIZE_FAILED,
                capture(analyticsSlot),
                true
            )
        }
        assertFalse(analyticsSlot.captured.isVaultRequest)
        assertEquals(expectedAnalyticsParams.appSwitchUrl, analyticsSlot.captured.appSwitchUrl)
        assertEquals(graphQLError.message, analyticsSlot.captured.errorDescription)
        verify { analyticsParamRepository.reset() }
    }

    @Test
    fun tokenize_withPaymentContext_performsVaultRequestIfRequestPersisted() {
        var braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(venmoEnabledConfiguration)
            .build()

        every { browserSwitchResult.returnUrl } returns SUCCESS_URL
        every { merchantRepository.authorization } returns clientToken

        var nonce = mockk<VenmoAccountNonce>()
        every { nonce.string } returns "some-nonce"

        venmoApi = MockkVenmoApiBuilder()
            .createNonceFromPaymentContextSuccess(nonce)
            .build()

        every { sharedPrefsWriter.getVenmoVaultOption(context) } returns true

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.tokenize(paymentAuthResult, venmoTokenizeCallback)

        var callbackSlot = slot<VenmoInternalCallback>()
        verify { venmoApi.vaultVenmoAccountNonce( "some-nonce", capture(callbackSlot)) }
    }

    @Test
    fun tokenize_postsPaymentMethodNonceOnSuccess() {
        var braintreeClient = MockkBraintreeClientBuilder().build()

        every { browserSwitchResult.returnUrl } returns SUCCESS_URL
        every { merchantRepository.authorization } returns clientToken

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.tokenize(paymentAuthResult, venmoTokenizeCallback)

        verify { venmoApi.createNonceFromPaymentContext("a-resource-id", any<VenmoInternalCallback>()) }
    }

    @Test
    fun tokenize_performsVaultRequestIfRequestPersisted() {
        var braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(venmoEnabledConfiguration)
            .build()

        every { browserSwitchResult.returnUrl } returns SUCCESS_URL
        every { merchantRepository.authorization } returns clientToken

        var venmoApi = MockkVenmoApiBuilder()
            .createNonceFromPaymentContextSuccess(VenmoAccountNonce.fromJSON(
                JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE)))
            .build()

        every { sharedPrefsWriter.getVenmoVaultOption(context) } returns true

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.tokenize(paymentAuthResult, venmoTokenizeCallback)

        verify { venmoApi.vaultVenmoAccountNonce("fake-venmo-nonce", any<VenmoInternalCallback>()) }
    }

    @Test
    fun tokenize_doesNotPerformRequestIfTokenizationKeyUsed() {
        var braintreeClient = MockkBraintreeClientBuilder().build()

        every { browserSwitchResult.returnUrl } returns SUCCESS_URL
        every { sharedPrefsWriter.getVenmoVaultOption(context) } returns true
        every { merchantRepository.authorization } returns tokenizationKey

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.tokenize(paymentAuthResult, venmoTokenizeCallback)

        verify(exactly = 0) { venmoApi.vaultVenmoAccountNonce(any<String>(),
            any<VenmoInternalCallback>()) }
    }

    @Test
    fun tokenize_withSuccessfulVaultCall_forwardsResultToActivityResultListener_andSendsAnalytics() {
        var braintreeClient = MockkBraintreeClientBuilder().build()

        every { browserSwitchResult.returnUrl } returns SUCCESS_URL_WITHOUT_RESOURCE_ID
        every { merchantRepository.authorization } returns clientToken

        var venmoAccountNonce = mockk<VenmoAccountNonce>()
        venmoApi = MockkVenmoApiBuilder()
            .vaultVenmoAccountNonceSuccess(venmoAccountNonce)
            .build()

        every { sharedPrefsWriter.getVenmoVaultOption(context) } returns true

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.tokenize(paymentAuthResult, venmoTokenizeCallback)

        var resultSlot = slot<VenmoResult>()
        verify { venmoTokenizeCallback.onVenmoResult(capture(resultSlot)) }
        assertTrue(resultSlot.captured is VenmoResult.Success)
        var nonce = (resultSlot.captured as VenmoResult.Success).nonce
        assertEquals(venmoAccountNonce, nonce)
        verify {
            braintreeClient.sendAnalyticsEvent(
                VenmoAnalytics.TOKENIZE_SUCCEEDED,
                expectedVaultAnalyticsParams,
                true
            )
        }
    }

    @Test
    fun tokenize_withPaymentContext_withSuccessfulVaultCall_forwardsNonceToCallback_andSendsAnalytics() {
        var braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPOSTSuccessfulResponse(Fixtures.VENMO_GRAPHQL_GET_PAYMENT_CONTEXT_RESPONSE)
            .build()

        every { browserSwitchResult.returnUrl } returns SUCCESS_URL
        every { merchantRepository.authorization } returns clientToken

        var venmoAccountNonce = VenmoAccountNonce.fromJSON(
            JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE))

        venmoApi = MockkVenmoApiBuilder()
            .createNonceFromPaymentContextSuccess(venmoAccountNonce)
            .vaultVenmoAccountNonceSuccess(venmoAccountNonce)
            .build()

        every { sharedPrefsWriter.getVenmoVaultOption(context) } returns true

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.tokenize(paymentAuthResult, venmoTokenizeCallback)

        var resultSlot = slot<VenmoResult>()
        verify { venmoTokenizeCallback.onVenmoResult(capture(resultSlot)) }
        assertTrue(resultSlot.captured is VenmoResult.Success)
        var nonce = (resultSlot.captured as VenmoResult.Success).nonce
        assertEquals(venmoAccountNonce, nonce)
        verify {
            braintreeClient.sendAnalyticsEvent(
                VenmoAnalytics.TOKENIZE_SUCCEEDED,
                expectedVaultAnalyticsParams,
                true
            )
        }
    }

    @Test
    fun tokenize_withFailedVaultCall_forwardsErrorToActivityResultListener_andSendsAnalytics() {
        var braintreeClient = MockkBraintreeClientBuilder().build()

        every { browserSwitchResult.returnUrl } returns SUCCESS_URL_WITHOUT_RESOURCE_ID
        every { merchantRepository.authorization } returns clientToken

        var error = Exception("error")
        venmoApi = MockkVenmoApiBuilder()
            .vaultVenmoAccountNonceError(error)
            .build()

        every { sharedPrefsWriter.getVenmoVaultOption(context) } returns true

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.tokenize(paymentAuthResult, venmoTokenizeCallback)

        var resultSlot = slot<VenmoResult>()
        verify { venmoTokenizeCallback.onVenmoResult(capture(resultSlot)) }
        assertTrue(resultSlot.captured is VenmoResult.Failure)
        assertEquals(error, (resultSlot.captured as VenmoResult.Failure).error)

        var analyticsSlot = slot<AnalyticsEventParams>()
        verify {
            braintreeClient.sendAnalyticsEvent(
                VenmoAnalytics.TOKENIZE_FAILED,
                capture(analyticsSlot),
                true
            )
        }
        assertEquals(expectedVaultAnalyticsParams.appSwitchUrl, analyticsSlot.captured.appSwitchUrl)
        assertEquals(error.message, analyticsSlot.captured.errorDescription)
    }

    @Test
    fun tokenize_withPaymentContext_withFailedVaultCall_forwardsErrorToCallback_andSendsAnalytics() {
        var braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPOSTSuccessfulResponse(Fixtures.VENMO_GRAPHQL_GET_PAYMENT_CONTEXT_RESPONSE)
            .build()

        every { browserSwitchResult.returnUrl } returns SUCCESS_URL
        every { merchantRepository.authorization } returns clientToken

        var venmoAccountNonce = VenmoAccountNonce.fromJSON(
            JSONObject(Fixtures.PAYMENT_METHODS_VENMO_ACCOUNT_RESPONSE))
        var error = Exception("error")

        venmoApi = MockkVenmoApiBuilder()
            .createNonceFromPaymentContextSuccess(venmoAccountNonce)
            .vaultVenmoAccountNonceError(error)
            .build()

        every { sharedPrefsWriter.getVenmoVaultOption(context) } returns true

        sut = VenmoClient(
            braintreeClient,
            apiClient,
            venmoApi,
            sharedPrefsWriter,
            analyticsParamRepository,
            merchantRepository,
            venmoRepository,
            getReturnLinkTypeUseCase,
            getReturnLinkUseCase
        )
        sut.tokenize(paymentAuthResult, venmoTokenizeCallback)

        var resultSlot = slot<VenmoResult>()
        verify { venmoTokenizeCallback.onVenmoResult(capture(resultSlot)) }
        assertTrue(resultSlot.captured is VenmoResult.Failure)
        assertEquals(error, (resultSlot.captured as VenmoResult.Failure).error)

        var analyticsSlot = slot<AnalyticsEventParams>()
        verify {
            braintreeClient.sendAnalyticsEvent(
                VenmoAnalytics.TOKENIZE_FAILED,
                capture(analyticsSlot),
                true
            )
        }
        assertEquals(expectedVaultAnalyticsParams.appSwitchUrl, analyticsSlot.captured.appSwitchUrl)
        assertEquals(error.message, analyticsSlot.captured.errorDescription)
    }
}