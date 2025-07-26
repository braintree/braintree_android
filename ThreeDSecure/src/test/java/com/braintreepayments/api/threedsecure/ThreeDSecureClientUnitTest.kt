package com.braintreepayments.api.threedsecure

import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.core.*
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import com.braintreepayments.api.testutils.TestConfigurationBuilder
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalActionCode
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse

@RunWith(RobolectricTestRunner::class)
class ThreeDSecureClientUnitTest {

    private val activity: FragmentActivity = mockk(relaxed = true)
    private val threeDSecureAPI: ThreeDSecureAPI = mockk(relaxed = true)
    private val merchantRepository: MerchantRepository = mockk(relaxed = true)
    private val paymentAuthRequestCallback: ThreeDSecurePaymentAuthRequestCallback = mockk(relaxed = true)
    private val threeDSecureTokenizeCallback: ThreeDSecureTokenizeCallback = mockk(relaxed = true)

    private lateinit var threeDSecureEnabledConfig: Configuration
    private lateinit var basicRequest: ThreeDSecureRequest
    private lateinit var threeDSecureParams: ThreeDSecureParams

    @Before
    fun setUp() {
        threeDSecureEnabledConfig = Configuration.fromJson(TestConfigurationBuilder()
            .threeDSecureEnabled(true)
            .cardinalAuthenticationJWT("cardinal-jwt")
            .build())

        basicRequest = ThreeDSecureRequest().apply {
            nonce = "a-nonce"
            amount = "amount"
            billingAddress = ThreeDSecurePostalAddress().apply {
                givenName = "billing-given-name"
            }
        }

        threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE)

        every { merchantRepository.authorization } returns Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN)
    }

    @Test
    fun prepareLookup_returnsValidLookupJSONString() {
        val cardinalClient = MockkCardinalClientBuilder()
            .successReferenceId("fake-df")
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            ThreeDSecureAPI(braintreeClient),
            merchantRepository
        )

        val callback = mockk<ThreeDSecurePrepareLookupCallback>(relaxed = true)
        sut.prepareLookup(activity, basicRequest, callback)

        val captor = slot<ThreeDSecurePrepareLookupResult>()
        verify { callback.onPrepareLookupResult(capture(captor)) }

        val prepareLookupResult = captor.captured
        assertTrue(prepareLookupResult is ThreeDSecurePrepareLookupResult.Success)
        assertSame(basicRequest, prepareLookupResult.request)

        val clientData = prepareLookupResult.clientData
        val lookup = JSONObject(clientData)
        assertEquals("encoded_auth_fingerprint", lookup.getString("authorizationFingerprint"))
        assertEquals(
            "Android-${com.braintreepayments.api.core.BuildConfig.VERSION_NAME}",
            lookup.getString("braintreeLibraryVersion")
        )
        assertEquals("fake-df", lookup.getString("dfReferenceId"))
        assertEquals("a-nonce", lookup.getString("nonce"))

        val clientMetaData = lookup.getJSONObject("clientMetadata")
        assertEquals("2", clientMetaData.getString("requestedThreeDSecureVersion"))
        assertEquals(
            "Android/${com.braintreepayments.api.core.BuildConfig.VERSION_NAME}",
            clientMetaData.getString("sdkVersion")
        )
    }

    @Test
    fun prepareLookup_initializesCardinal() {
        val cardinalClient = MockkCardinalClientBuilder()
            .successReferenceId("fake-df")
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            ThreeDSecureAPI(braintreeClient),
            merchantRepository
        )

        val callback = mockk<ThreeDSecurePrepareLookupCallback>(relaxed = true)
        sut.prepareLookup(activity, basicRequest, callback)

        verify {
            cardinalClient.initialize(
                activity,
                threeDSecureEnabledConfig,
                basicRequest,
                any<CardinalInitializeCallback>()
            )
        }
    }

    @Test
    fun prepareLookup_whenCardinalClientInitializeFails_forwardsError() {
        val initializeRuntimeError = BraintreeException("initialize error")
        val cardinalClient = MockkCardinalClientBuilder()
            .initializeRuntimeError(initializeRuntimeError)
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            ThreeDSecureAPI(braintreeClient),
            merchantRepository
        )

        val callback = mockk<ThreeDSecurePrepareLookupCallback>(relaxed = true)
        sut.prepareLookup(activity, basicRequest, callback)

        val captor = slot<ThreeDSecurePrepareLookupResult>()
        verify { callback.onPrepareLookupResult(capture(captor)) }

        val prepareLookupResult = captor.captured
        assertTrue(prepareLookupResult is ThreeDSecurePrepareLookupResult.Failure)
        assertEquals(initializeRuntimeError, prepareLookupResult.error)
    }

    @Test
    fun prepareLookup_whenDfReferenceIdMissing_forwardsError() {
        val cardinalClient = MockkCardinalClientBuilder()
            .successReferenceId("")
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            ThreeDSecureAPI(braintreeClient),
            merchantRepository
        )

        val callback = mockk<ThreeDSecurePrepareLookupCallback>(relaxed = true)
        sut.prepareLookup(activity, basicRequest, callback)

        val captor = slot<ThreeDSecurePrepareLookupResult>()
        verify { callback.onPrepareLookupResult(capture(captor)) }

        val prepareLookupResult = captor.captured
        assertTrue(prepareLookupResult is ThreeDSecurePrepareLookupResult.Failure)
        val error = prepareLookupResult.error

        assertTrue(error is BraintreeException)
        assertEquals("There was an error retrieving the dfReferenceId.", error.message)
    }

    @Test
    fun prepareLookup_withoutCardinalJWT_postsException() {
        val cardinalClient = MockkCardinalClientBuilder().build()
        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .threeDSecureEnabled(true)
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            ThreeDSecureAPI(braintreeClient),
            merchantRepository
        )

        val callback = mockk<ThreeDSecurePrepareLookupCallback>(relaxed = true)
        sut.prepareLookup(activity, basicRequest, callback)

        val captor = slot<ThreeDSecurePrepareLookupResult>()
        verify { callback.onPrepareLookupResult(capture(captor)) }
        val prepareLookupResult = captor.captured
        assertTrue(prepareLookupResult is ThreeDSecurePrepareLookupResult.Failure)
        val error = prepareLookupResult.error

        assertTrue(error is BraintreeException)
        assertEquals(
            "Merchant is not configured for 3DS 2.0. " +
                    "Please contact Braintree Support for assistance.",
            error.message
        )
    }

    @Test
    fun createPaymentAuthRequest_sendsAnalyticEvent() {
        val cardinalClient = MockkCardinalClientBuilder()
            .successReferenceId("sample-session-id")
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository
        )
        sut.createPaymentAuthRequest(activity, basicRequest, paymentAuthRequestCallback)

        verify { braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_STARTED) }
    }

    @Test
    fun createPaymentAuthRequest_sendsParamsInLookupRequest() {
        val cardinalClient = MockkCardinalClientBuilder()
            .successReferenceId("df-reference-id")
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .build()

        val request = ThreeDSecureRequest().apply {
            nonce = "a-nonce"
            amount = "amount"
            requestedExemptionType = ThreeDSecureRequestedExemptionType.SECURE_CORPORATE
            billingAddress = ThreeDSecurePostalAddress().apply {
                givenName = "billing-given-name"
            }
        }

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            ThreeDSecureAPI(braintreeClient),
            merchantRepository
        )
        sut.createPaymentAuthRequest(activity, request, paymentAuthRequestCallback)

        val bodyCaptor = slot<String>()
        verify {
            braintreeClient.sendPOST(
                "/v1/payment_methods/a-nonce/three_d_secure/lookup",
                capture(bodyCaptor),
                any(),
                any()
            )
        }

        val body = JSONObject(bodyCaptor.captured)
        assertEquals("amount", body.getString("amount"))
        assertEquals("df-reference-id", body.getString("df_reference_id"))
        assertEquals("billing-given-name", body.getJSONObject("additional_info").getString("billing_given_name"))
        assertEquals("secure_corporate", body.getString("requested_exemption_type"))
    }

    @Test
    fun createPaymentAuthRequest_performsLookup_WhenCardinalSDKInitFails() {
        val cardinalClient = MockkCardinalClientBuilder()
            .error(Exception("error"))
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .build()

        val request = ThreeDSecureRequest().apply {
            nonce = "a-nonce"
            amount = "amount"
            billingAddress = ThreeDSecurePostalAddress().apply {
                givenName = "billing-given-name"
            }
        }

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            ThreeDSecureAPI(braintreeClient),
            merchantRepository
        )
        sut.createPaymentAuthRequest(activity, request, paymentAuthRequestCallback)

        val pathCaptor = slot<String>()
        val bodyCaptor = slot<String>()
        verify {
            braintreeClient.sendPOST(capture(pathCaptor), capture(bodyCaptor), any(), any())
        }

        val path = pathCaptor.captured
        val body = bodyCaptor.captured
        val bodyJson = JSONObject(body)

        assertEquals("/v1/payment_methods/a-nonce/three_d_secure/lookup", path)
        assertEquals("amount", bodyJson.get("amount"))
        assertFalse(bodyJson.getBoolean("challenge_requested"))
        assertFalse(bodyJson.getBoolean("data_only_requested"))
        assertFalse(bodyJson.getBoolean("exemption_requested"))
        val additionalInfo = bodyJson.getJSONObject("additional_info")
        assertEquals("billing-given-name", additionalInfo.get("billing_given_name"))
    }

    @Test
    fun createPaymentAuthRequest_callsLookupListener() {
        val cardinalClient = MockkCardinalClientBuilder()
            .successReferenceId("sample-session-id")
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .sendPostSuccessfulResponse(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE)
            .build()

        val request = ThreeDSecureRequest().apply {
            nonce = "a-nonce"
            amount = "amount"
            billingAddress = ThreeDSecurePostalAddress().apply {
                givenName = "billing-given-name"
            }
        }

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            ThreeDSecureAPI(braintreeClient),
            merchantRepository
        )

        sut.createPaymentAuthRequest(activity, request, paymentAuthRequestCallback)

        verify { paymentAuthRequestCallback.onThreeDSecurePaymentAuthRequest(any<ThreeDSecurePaymentAuthRequest>()) }
    }

    @Test
    fun createPaymentAuthRequest_withInvalidRequest_postsException() {
        val cardinalClient = MockkCardinalClientBuilder().build()

        val braintreeClient = MockkBraintreeClientBuilder().build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository
        )

        val request = ThreeDSecureRequest().apply {
            amount = "5"
        }
        sut.createPaymentAuthRequest(activity, request, paymentAuthRequestCallback)

        val captor = slot<ThreeDSecurePaymentAuthRequest>()
        verify { paymentAuthRequestCallback.onThreeDSecurePaymentAuthRequest(capture(captor)) }
        val paymentAuthRequest = captor.captured
        assertTrue(paymentAuthRequest is ThreeDSecurePaymentAuthRequest.Failure)
        assertEquals(
            "The ThreeDSecureRequest nonce and amount cannot be null",
            paymentAuthRequest.error.message
        )
    }

    @Test
    fun createPaymentAuthRequest_initializesCardinal() {
        val cardinalClient = MockkCardinalClientBuilder()
            .successReferenceId("df-reference-id")
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            ThreeDSecureAPI(braintreeClient),
            merchantRepository
        )
        sut.createPaymentAuthRequest(activity, basicRequest, paymentAuthRequestCallback)

        verify { cardinalClient.initialize(activity, threeDSecureEnabledConfig, basicRequest, any()) }
    }

    @Test
    fun createPaymentAuthRequest_whenCardinalClientInitializeFails_forwardsError() {
        val initializeRuntimeError = BraintreeException("initialize error")
        val cardinalClient = MockkCardinalClientBuilder()
            .initializeRuntimeError(initializeRuntimeError)
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            ThreeDSecureAPI(braintreeClient),
            merchantRepository
        )

        sut.createPaymentAuthRequest(activity, basicRequest, paymentAuthRequestCallback)

        val captor = slot<ThreeDSecurePaymentAuthRequest>()
        verify { paymentAuthRequestCallback.onThreeDSecurePaymentAuthRequest(capture(captor)) }
        val paymentAuthRequest = captor.captured
        assertTrue(paymentAuthRequest is ThreeDSecurePaymentAuthRequest.Failure)
        assertEquals(initializeRuntimeError, paymentAuthRequest.error)
    }

    @Test
    fun createPaymentAuthRequest_whenCardinalSetupFailed_sendsAnalyticEvent() {
        val cardinalClient = MockkCardinalClientBuilder()
            .initializeRuntimeError(BraintreeException("cardinal error"))
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            ThreeDSecureAPI(braintreeClient),
            merchantRepository
        )
        sut.createPaymentAuthRequest(activity, basicRequest, paymentAuthRequestCallback)

        val verifyCaptor = slot<AnalyticsEventParams>()

        verify { braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_STARTED) }
        verify { braintreeClient.sendAnalyticsEvent(eq(ThreeDSecureAnalytics.VERIFY_FAILED), capture(verifyCaptor)) }
        assertEquals("cardinal error", verifyCaptor.captured.errorDescription)
    }

    @Test
    fun createPaymentAuthRequest_withoutCardinalJWT_postsException() {
        val cardinalClient = MockkCardinalClientBuilder().build()

        val configuration = Configuration.fromJson(TestConfigurationBuilder()
            .threeDSecureEnabled(true)
            .build())

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            ThreeDSecureAPI(braintreeClient),
            merchantRepository
        )

        sut.createPaymentAuthRequest(activity, basicRequest, paymentAuthRequestCallback)

        val captor = slot<ThreeDSecurePaymentAuthRequest>()
        verify { paymentAuthRequestCallback.onThreeDSecurePaymentAuthRequest(capture(captor)) }
        val paymentAuthRequest = captor.captured
        assertTrue(paymentAuthRequest is ThreeDSecurePaymentAuthRequest.Failure)
        val error = paymentAuthRequest.error
        assertTrue(error is BraintreeException)
        assertEquals(
            "Merchant is not configured for 3DS 2.0. Please contact Braintree Support for assistance.",
            error.message
        )
    }

    @Test
    fun sendAnalyticsAndCallbackResult_whenAuthenticatingWithCardinal_sendsAnalyticsEvent() {
        val cardinalClient = MockkCardinalClientBuilder()
            .successReferenceId("reference-id")
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            ThreeDSecureAPI(braintreeClient),
            merchantRepository
        )

        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE)
        sut.sendAnalyticsAndCallbackResult(threeDSecureParams, paymentAuthRequestCallback)

        verify { braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.LOOKUP_SUCCEEDED) }
    }

    @Test
    fun sendAnalyticsAndCallbackResult_whenChallengeIsRequired_sendsAnalyticsEvent() {
        val cardinalClient = MockkCardinalClientBuilder()
            .successReferenceId("reference-id")
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .returnUrlScheme("sample-return-url://")
            .build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            ThreeDSecureAPI(braintreeClient),
            merchantRepository
        )

        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE)
        sut.sendAnalyticsAndCallbackResult(threeDSecureParams, paymentAuthRequestCallback)

        verify { braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.LOOKUP_SUCCEEDED) }
        verify { braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.CHALLENGE_REQUIRED) }
    }

    @Test
    fun sendAnalyticsAndCallbackResult_whenChallengeIsNotPresented_returnsResult() {
        val cardinalClient = MockkCardinalClientBuilder()
            .successReferenceId("reference-id")
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            ThreeDSecureAPI(braintreeClient),
            merchantRepository
        )

        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE_NO_ACS_URL)
        sut.sendAnalyticsAndCallbackResult(threeDSecureParams, paymentAuthRequestCallback)

        val captor = slot<ThreeDSecurePaymentAuthRequest>()
        verify { paymentAuthRequestCallback.onThreeDSecurePaymentAuthRequest(capture(captor)) }
        val paymentAuthRequest = captor.captured
        assertTrue(paymentAuthRequest is ThreeDSecurePaymentAuthRequest.LaunchNotRequired)
        assertEquals(threeDSecureParams.threeDSecureNonce, paymentAuthRequest.nonce)
        assertEquals(threeDSecureParams.lookup, paymentAuthRequest.threeDSecureLookup)
    }

    @Test
    fun sendAnalyticsAndCallbackResult_callsBackThreeDSecureResultForLaunch() {
        val cardinalClient = MockkCardinalClientBuilder()
            .successReferenceId("reference-id")
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            ThreeDSecureAPI(braintreeClient),
            merchantRepository
        )

        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE)
        sut.sendAnalyticsAndCallbackResult(threeDSecureParams, paymentAuthRequestCallback)

        val captor = slot<ThreeDSecurePaymentAuthRequest>()
        verify { paymentAuthRequestCallback.onThreeDSecurePaymentAuthRequest(capture(captor)) }
        val paymentAuthRequest = captor.captured
        assertTrue(paymentAuthRequest is ThreeDSecurePaymentAuthRequest.ReadyToLaunch)
        assertEquals(threeDSecureParams, paymentAuthRequest.requestParams)
    }

    @Test
    fun tokenize_whenErrorExists_forwardsErrorToCallback_andSendsAnalytics() {
        val cardinalClient = MockkCardinalClientBuilder().build()
        val braintreeClient = MockkBraintreeClientBuilder().build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository
        )

        val threeDSecureError = Exception("3DS error.")
        val paymentAuthResult = ThreeDSecurePaymentAuthResult(null, null, null, threeDSecureError)
        sut.tokenize(paymentAuthResult, threeDSecureTokenizeCallback)

        val captor = slot<ThreeDSecureResult>()
        verify { threeDSecureTokenizeCallback.onThreeDSecureResult(capture(captor)) }
        val result = captor.captured
        assertTrue(result is ThreeDSecureResult.Failure)
        assertEquals(threeDSecureError, result.error)
    }

    @Test
    fun tokenize_whenValidateResponseTimeout_returnsErrorAndSendsAnalytics() {
        val errorMessage = "Error"
        val cardinalClient = MockkCardinalClientBuilder().build()
        val braintreeClient = MockkBraintreeClientBuilder().build()

        val validateResponse = mockk<ValidateResponse> {
            every { actionCode } returns CardinalActionCode.TIMEOUT
            every { errorDescription } returns errorMessage
        }

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository
        )

        val paymentAuthResult = ThreeDSecurePaymentAuthResult("jwt", validateResponse, threeDSecureParams, null)
        sut.tokenize(paymentAuthResult, threeDSecureTokenizeCallback)

        val captor = slot<ThreeDSecureResult>()
        verify { threeDSecureTokenizeCallback.onThreeDSecureResult(capture(captor)) }
        val result = captor.captured
        assertTrue(result is ThreeDSecureResult.Failure)

        val error = result.error
        assertTrue(error is BraintreeException)
        assertEquals(errorMessage, error.message)

        val paramCaptor = slot<AnalyticsEventParams>()
        verify { braintreeClient.sendAnalyticsEvent(eq(ThreeDSecureAnalytics.VERIFY_FAILED), capture(paramCaptor)) }
        assertEquals(errorMessage, paramCaptor.captured.errorDescription)
    }

    @Test
    fun tokenize_whenValidateResponseCancel_returnsUserCanceledErrorAndSendsAnalytics() {
        val cardinalClient = MockkCardinalClientBuilder().build()
        val braintreeClient = MockkBraintreeClientBuilder().build()

        val validateResponse = mockk<ValidateResponse> {
            every { actionCode } returns CardinalActionCode.CANCEL
        }

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository
        )

        val paymentAuthResult = ThreeDSecurePaymentAuthResult("jwt", validateResponse, threeDSecureParams, null)
        sut.tokenize(paymentAuthResult, threeDSecureTokenizeCallback)

        val captor = slot<ThreeDSecureResult>()
        verify { threeDSecureTokenizeCallback.onThreeDSecureResult(capture(captor)) }
        val result = captor.captured
        assertTrue(result is ThreeDSecureResult.Cancel)

        verify { braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_CANCELED) }
    }

    @Test
    fun tokenize_whenValidateResponseSuccess_onAuthenticateCardinalJWTResult_returnsResultAndSendsAnalytics() {
        val cardinalClient = MockkCardinalClientBuilder().build()
        val braintreeClient = MockkBraintreeClientBuilder().build()

        val validateResponse = mockk<ValidateResponse> {
            every { actionCode } returns CardinalActionCode.SUCCESS
        }

        every {
            threeDSecureAPI.authenticateCardinalJWT(any(), any(), any())
        } answers { call ->
            val callback = call.invocation.args[2] as ThreeDSecureResultCallback
            callback.onThreeDSecureResult(threeDSecureParams, null)
        }

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository
        )

        val paymentAuthResult = ThreeDSecurePaymentAuthResult("jwt", validateResponse, threeDSecureParams, null)
        sut.tokenize(paymentAuthResult, threeDSecureTokenizeCallback)

        val captor = slot<ThreeDSecureResult>()
        verify { threeDSecureTokenizeCallback.onThreeDSecureResult(capture(captor)) }
        val result = captor.captured
        assertTrue(result is ThreeDSecureResult.Success)
        assertEquals(result.nonce, threeDSecureParams.threeDSecureNonce)

        verify { braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.JWT_AUTH_SUCCEEDED) }
        verify { braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_SUCCEEDED) }
    }

    @Test
    fun tokenize_whenValidateResponseSuccess_onAuthenticateCardinalJWTResultWithError_returnsResultAndSendsAnalytics() {
        val cardinalClient = MockkCardinalClientBuilder().build()
        val braintreeClient = MockkBraintreeClientBuilder().build()

        val validateResponse = mockk<ValidateResponse> {
            every { actionCode } returns CardinalActionCode.SUCCESS
        }

        val threeDSecureParams = mockk<ThreeDSecureParams>(relaxed = true) {
            every { hasError() } returns true
        }

        every {
            threeDSecureAPI.authenticateCardinalJWT(any(), any(), any())
        } answers { call ->
            val callback = call.invocation.args[2] as ThreeDSecureResultCallback
            callback.onThreeDSecureResult(threeDSecureParams, null)
        }

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository
        )

        val paymentAuthResult = ThreeDSecurePaymentAuthResult("jwt", validateResponse, threeDSecureParams, null)
        sut.tokenize(paymentAuthResult, threeDSecureTokenizeCallback)

        val captorList = mutableListOf<ThreeDSecureResult>()
        verify(exactly = 1) { threeDSecureTokenizeCallback.onThreeDSecureResult(capture(captorList)) }
        val result = captorList.first()
        assertTrue(result is ThreeDSecureResult.Failure)
        assertEquals(result.nonce, paymentAuthResult.threeDSecureParams?.threeDSecureNonce)

        verify {
            braintreeClient.sendAnalyticsEvent(
                ThreeDSecureAnalytics.JWT_AUTH_FAILED,
                any<AnalyticsEventParams>()
            )
        }
        verify {
            braintreeClient.sendAnalyticsEvent(
                ThreeDSecureAnalytics.JWT_AUTH_FAILED,
                any<AnalyticsEventParams>()
            )
        }
    }

    @Test
    fun tokenize_whenValidateResponseSuccess_onAuthenticateCardinalJWTError_returnsErrorAndSendsAnalytics() {
        val cardinalClient = MockkCardinalClientBuilder().build()
        val braintreeClient = MockkBraintreeClientBuilder().build()

        val validateResponse = mockk<ValidateResponse> {
            every { actionCode } returns CardinalActionCode.SUCCESS
        }

        val exceptionMessage = "error"
        val exception = Exception(exceptionMessage)

        every {
            threeDSecureAPI.authenticateCardinalJWT(any(), any(), any())
        } answers { call ->
            val callback = call.invocation.args[2] as ThreeDSecureResultCallback
            callback.onThreeDSecureResult(null, exception)
        }

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository
        )

        val paymentAuthResult = ThreeDSecurePaymentAuthResult("jwt", validateResponse, threeDSecureParams, null)
        sut.tokenize(paymentAuthResult, threeDSecureTokenizeCallback)

        val captor = slot<ThreeDSecureResult>()
        verify { threeDSecureTokenizeCallback.onThreeDSecureResult(capture(captor)) }
        val result = captor.captured
        assertTrue(result is ThreeDSecureResult.Failure)
        assertEquals(exception, result.error)

        val jwtCaptor = slot<AnalyticsEventParams>()
        val verifyCaptor = slot<AnalyticsEventParams>()

        verify { braintreeClient.sendAnalyticsEvent(eq(ThreeDSecureAnalytics.JWT_AUTH_FAILED), capture(jwtCaptor)) }
        verify { braintreeClient.sendAnalyticsEvent(eq(ThreeDSecureAnalytics.VERIFY_FAILED), capture(verifyCaptor)) }
        assertEquals(exceptionMessage, jwtCaptor.captured.errorDescription)
        assertEquals(exceptionMessage, verifyCaptor.captured.errorDescription)
    }
}
