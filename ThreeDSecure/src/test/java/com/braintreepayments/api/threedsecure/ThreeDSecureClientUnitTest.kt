package com.braintreepayments.api.threedsecure

import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import com.braintreepayments.api.testutils.TestConfigurationBuilder
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalActionCode
import com.cardinalcommerce.cardinalmobilesdk.models.ValidateResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Suppress("MaxLineLength")
class ThreeDSecureClientUnitTest {
    private val testDispatcher = StandardTestDispatcher()

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
    fun `when prepareLookup succeeds, returns a valid lookup JSON string containing request data`() = runTest(testDispatcher) {
        val cardinalClient = MockkCardinalClientBuilder()
            .successReferenceId("fake-df")
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )

        val callback = mockk<ThreeDSecurePrepareLookupCallback>(relaxed = true)
        sut.prepareLookup(activity, basicRequest, callback)

        advanceUntilIdle()

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
    fun `when prepareLookup is called, initializes the cardinal client`() = runTest(testDispatcher) {
        val cardinalClient = MockkCardinalClientBuilder()
            .successReferenceId("fake-df")
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )

        val callback = mockk<ThreeDSecurePrepareLookupCallback>(relaxed = true)
        sut.prepareLookup(activity, basicRequest, callback)

        advanceUntilIdle()

        coVerify {
            cardinalClient.initialize(
                activity,
                threeDSecureEnabledConfig,
                basicRequest
            )
        }
    }

    @Test
    fun `when cardinal client initialize fails during prepareLookup, forwards the error to the callback`() = runTest(testDispatcher) {
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
            threeDSecureAPI,
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )

        val callback = mockk<ThreeDSecurePrepareLookupCallback>(relaxed = true)
        sut.prepareLookup(activity, basicRequest, callback)

        advanceUntilIdle()

        val captor = slot<ThreeDSecurePrepareLookupResult>()
        verify { callback.onPrepareLookupResult(capture(captor)) }

        val prepareLookupResult = captor.captured
        assertTrue(prepareLookupResult is ThreeDSecurePrepareLookupResult.Failure)
        assertEquals(initializeRuntimeError, prepareLookupResult.error)
    }

    @Test
    fun `when dfReferenceId is missing during prepareLookup, forwards a BraintreeException to the callback`() = runTest(testDispatcher) {
        val cardinalClient = MockkCardinalClientBuilder()
            .successReferenceId("")
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )

        val callback = mockk<ThreeDSecurePrepareLookupCallback>(relaxed = true)
        sut.prepareLookup(activity, basicRequest, callback)

        advanceUntilIdle()

        val captor = slot<ThreeDSecurePrepareLookupResult>()
        verify { callback.onPrepareLookupResult(capture(captor)) }

        val prepareLookupResult = captor.captured
        assertTrue(prepareLookupResult is ThreeDSecurePrepareLookupResult.Failure)
        val error = prepareLookupResult.error

        assertTrue(error is BraintreeException)
        assertEquals("There was an error retrieving the dfReferenceId.", error.message)
    }

    @Test
    fun `when merchant is not configured for 3D Secure version 2, prepareLookup posts a BraintreeException`() = runTest(testDispatcher) {
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
            threeDSecureAPI,
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )

        val callback = mockk<ThreeDSecurePrepareLookupCallback>(relaxed = true)
        sut.prepareLookup(activity, basicRequest, callback)

        advanceUntilIdle()

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
    fun `when createPaymentAuthRequest is called, sends verify started analytic event`() {
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
    fun `when createPaymentAuthRequest is called, sends request params including exemption type in the lookup POST body`() = runTest(testDispatcher) {
        val cardinalClient = MockkCardinalClientBuilder()
            .successReferenceId("df-reference-id")
            .build()

        val bodyCaptor = slot<String>()
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .sendPostSuccessfulResponse("{}")
            .build()

        coEvery {
            braintreeClient.sendPOST(
                url = eq("/v1/payment_methods/a-nonce/three_d_secure/lookup"),
                data = capture(bodyCaptor)
            )
        } returns "{}"

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
            ThreeDSecureAPI(
                braintreeClient
            ),
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )
        sut.createPaymentAuthRequest(activity, request, paymentAuthRequestCallback)
        advanceUntilIdle()
        val body = JSONObject(bodyCaptor.captured)
        assertEquals("amount", body.getString("amount"))
        assertEquals("df-reference-id", body.getString("df_reference_id"))
        assertEquals("billing-given-name", body.getJSONObject("additional_info").getString("billing_given_name"))
        assertEquals("secure_corporate", body.getString("requested_exemption_type"))
    }

    @Test
    fun `when cardinal SDK init fails, createPaymentAuthRequest still performs the lookup POST without df reference id`() = runTest(testDispatcher) {
        val cardinalClient = MockkCardinalClientBuilder()
            .error(Exception("error"))
            .build()

        val bodyCaptor = slot<String>()
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .sendPostSuccessfulResponse("{}")
            .build()

        coEvery {
            braintreeClient.sendPOST(
                url = eq("/v1/payment_methods/a-nonce/three_d_secure/lookup"),
                data = capture(bodyCaptor)
            )
        } returns "{}"

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
            ThreeDSecureAPI(
                braintreeClient
            ),
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )
        sut.createPaymentAuthRequest(activity, request, paymentAuthRequestCallback)
        advanceUntilIdle()
        val body = bodyCaptor.captured
        val bodyJson = JSONObject(body)

        assertEquals("amount", bodyJson.get("amount"))
        assertFalse(bodyJson.getBoolean("challenge_requested"))
        assertFalse(bodyJson.getBoolean("data_only_requested"))
        assertFalse(bodyJson.getBoolean("exemption_requested"))
        val additionalInfo = bodyJson.getJSONObject("additional_info")
        assertEquals("billing-given-name", additionalInfo.get("billing_given_name"))
    }

    @Test
    fun `when createPaymentAuthRequest completes the lookup, calls back the payment auth request listener`() = runTest(testDispatcher) {
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
            ThreeDSecureAPI(
                braintreeClient
            ),
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )

        sut.createPaymentAuthRequest(activity, request, paymentAuthRequestCallback)
        advanceUntilIdle()
        verify { paymentAuthRequestCallback.onThreeDSecurePaymentAuthRequest(any<ThreeDSecurePaymentAuthRequest>()) }
    }

    @Test
    fun `when the request nonce and amount are null, createPaymentAuthRequest posts a Failure with a BraintreeException`() = runTest(testDispatcher) {
        val cardinalClient = MockkCardinalClientBuilder().build()

        val braintreeClient = MockkBraintreeClientBuilder().build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )

        val request = ThreeDSecureRequest().apply {
            amount = "5"
        }
        sut.createPaymentAuthRequest(activity, request, paymentAuthRequestCallback)

        advanceUntilIdle()

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
    fun `when createPaymentAuthRequest is called, initializes the cardinal client`() = runTest(testDispatcher) {
        val cardinalClient = MockkCardinalClientBuilder()
            .successReferenceId("df-reference-id")
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )
        sut.createPaymentAuthRequest(activity, basicRequest, paymentAuthRequestCallback)

        advanceUntilIdle()

        coVerify { cardinalClient.initialize(activity, threeDSecureEnabledConfig, basicRequest) }
    }

    @Test
    fun `when cardinal client initialize fails during createPaymentAuthRequest, forwards the error as a Failure`() = runTest(testDispatcher) {
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
            threeDSecureAPI,
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )

        sut.createPaymentAuthRequest(activity, basicRequest, paymentAuthRequestCallback)

        advanceUntilIdle()

        val captor = slot<ThreeDSecurePaymentAuthRequest>()
        verify { paymentAuthRequestCallback.onThreeDSecurePaymentAuthRequest(capture(captor)) }
        val paymentAuthRequest = captor.captured
        assertTrue(paymentAuthRequest is ThreeDSecurePaymentAuthRequest.Failure)
        assertEquals(initializeRuntimeError, paymentAuthRequest.error)
    }

    @Test
    fun `when cardinal setup fails during createPaymentAuthRequest, sends verify started and verify failed analytics events`() = runTest(testDispatcher) {
        val cardinalClient = MockkCardinalClientBuilder()
            .initializeRuntimeError(BraintreeException("cardinal error"))
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )
        sut.createPaymentAuthRequest(activity, basicRequest, paymentAuthRequestCallback)

        advanceUntilIdle()

        val verifyCaptor = slot<AnalyticsEventParams>()

        verify { braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_STARTED) }
        verify { braintreeClient.sendAnalyticsEvent(eq(ThreeDSecureAnalytics.VERIFY_FAILED), capture(verifyCaptor)) }
        assertEquals("cardinal error", verifyCaptor.captured.errorDescription)
    }

    @Test
    fun `when merchant is not configured for 3D Secure version 2, createPaymentAuthRequest posts a Failure with a BraintreeException`() = runTest(testDispatcher) {
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
            threeDSecureAPI,
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )

        sut.createPaymentAuthRequest(activity, basicRequest, paymentAuthRequestCallback)

        advanceUntilIdle()

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
    fun `when sendAnalyticsAndResult is called, sends lookup succeeded analytics event`() = runTest(testDispatcher) {
        val cardinalClient = MockkCardinalClientBuilder()
            .successReferenceId("reference-id")
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )

        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE)
        sut.sendAnalyticsAndResult(threeDSecureParams)

        verify { braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.LOOKUP_SUCCEEDED) }
    }

    @Test
    fun `when the lookup requires a challenge, sendAnalyticsAndResult sends lookup succeeded and challenge required analytics events`() = runTest(testDispatcher) {
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
            threeDSecureAPI,
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )

        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE)
        sut.sendAnalyticsAndResult(threeDSecureParams)

        verify { braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.LOOKUP_SUCCEEDED) }
        verify { braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.CHALLENGE_REQUIRED) }
    }

    @Test
    fun `when the lookup has no acs url, sendAnalyticsAndResult returns a LaunchNotRequired result with the nonce and lookup`() = runTest(testDispatcher) {
        val cardinalClient = MockkCardinalClientBuilder()
            .successReferenceId("reference-id")
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )

        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_LOOKUP_RESPONSE_NO_ACS_URL)
        val result = sut.sendAnalyticsAndResult(threeDSecureParams)

        assertTrue(result is ThreeDSecurePaymentAuthRequest.LaunchNotRequired)
        assertEquals(threeDSecureParams.threeDSecureNonce, result.nonce)
        assertEquals(threeDSecureParams.lookup, result.threeDSecureLookup)
    }

    @Test
    fun `when a challenge is required, sendAnalyticsAndResult returns a ReadyToLaunch result with the request params`() = runTest(testDispatcher) {
        val cardinalClient = MockkCardinalClientBuilder()
            .successReferenceId("reference-id")
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )

        val threeDSecureParams = ThreeDSecureParams.fromJson(Fixtures.THREE_D_SECURE_V2_LOOKUP_RESPONSE)
        val result = sut.sendAnalyticsAndResult(threeDSecureParams)

        assertTrue(result is ThreeDSecurePaymentAuthRequest.ReadyToLaunch)
        assertEquals(threeDSecureParams, result.requestParams)
    }

    @Test
    fun `when the payment auth result contains an error, tokenize forwards the error to the callback`() = runTest(testDispatcher) {
        val cardinalClient = MockkCardinalClientBuilder().build()
        val braintreeClient = MockkBraintreeClientBuilder().build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )

        val threeDSecureError = Exception("3DS error.")
        val paymentAuthResult = ThreeDSecurePaymentAuthResult(null, null, null, threeDSecureError)
        sut.tokenize(paymentAuthResult, threeDSecureTokenizeCallback)
        advanceUntilIdle()

        val captor = slot<ThreeDSecureResult>()
        verify { threeDSecureTokenizeCallback.onThreeDSecureResult(capture(captor)) }
        val result = captor.captured
        assertTrue(result is ThreeDSecureResult.Failure)
        assertEquals(threeDSecureError, result.error)
    }

    @Test
    fun `when the validate response action code is timeout, tokenize returns a Failure and sends verify failed analytics with the error description`() = runTest(testDispatcher) {
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
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )

        val paymentAuthResult = ThreeDSecurePaymentAuthResult("jwt", validateResponse, threeDSecureParams, null)
        sut.tokenize(paymentAuthResult, threeDSecureTokenizeCallback)
        advanceUntilIdle()

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
    fun `when the validate response action code is cancel, tokenize returns a Cancel result and sends verify canceled analytics`() = runTest(testDispatcher) {
        val cardinalClient = MockkCardinalClientBuilder().build()
        val braintreeClient = MockkBraintreeClientBuilder().build()

        val validateResponse = mockk<ValidateResponse> {
            every { actionCode } returns CardinalActionCode.CANCEL
        }

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )

        val paymentAuthResult = ThreeDSecurePaymentAuthResult("jwt", validateResponse, threeDSecureParams, null)
        sut.tokenize(paymentAuthResult, threeDSecureTokenizeCallback)
        advanceUntilIdle()

        val captor = slot<ThreeDSecureResult>()
        verify { threeDSecureTokenizeCallback.onThreeDSecureResult(capture(captor)) }
        val result = captor.captured
        assertTrue(result is ThreeDSecureResult.Cancel)

        verify { braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_CANCELED) }
    }

    @Test
    fun `when the validate response action code is success and jwt authentication succeeds, tokenize returns a Success result and sends jwt and verify succeeded analytics`() =
        runTest(testDispatcher) {
        val cardinalClient = MockkCardinalClientBuilder().build()
        val braintreeClient = MockkBraintreeClientBuilder().build()

        val validateResponse = mockk<ValidateResponse> {
            every { actionCode } returns CardinalActionCode.SUCCESS
        }

        coEvery {
            threeDSecureAPI.authenticateCardinalJWT(any(), any())
        } returns threeDSecureParams

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )

        val paymentAuthResult = ThreeDSecurePaymentAuthResult("jwt", validateResponse, threeDSecureParams, null)
        sut.tokenize(paymentAuthResult, threeDSecureTokenizeCallback)
        advanceUntilIdle()

        val captor = slot<ThreeDSecureResult>()
        verify { threeDSecureTokenizeCallback.onThreeDSecureResult(capture(captor)) }
        val result = captor.captured
        assertTrue(result is ThreeDSecureResult.Success)
        assertEquals(result.nonce, threeDSecureParams.threeDSecureNonce)

        verify { braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.JWT_AUTH_SUCCEEDED) }
        verify { braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_SUCCEEDED) }
    }

    @Test
    fun `when the validate response action code is success but the jwt authentication result has an error, tokenize returns a Failure and sends jwt auth failed analytics`() =
        runTest(testDispatcher) {
        val cardinalClient = MockkCardinalClientBuilder().build()
        val braintreeClient = MockkBraintreeClientBuilder().build()

        val validateResponse = mockk<ValidateResponse> {
            every { actionCode } returns CardinalActionCode.SUCCESS
        }

        val threeDSecureParams = mockk<ThreeDSecureParams>(relaxed = true) {
            every { hasError() } returns true
        }

        coEvery {
            threeDSecureAPI.authenticateCardinalJWT(any(), any())
        } returns threeDSecureParams

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )

        val paymentAuthResult = ThreeDSecurePaymentAuthResult("jwt", validateResponse, threeDSecureParams, null)
        sut.tokenize(paymentAuthResult, threeDSecureTokenizeCallback)
        advanceUntilIdle()

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
    fun `when the validate response action code is success but authenticateCardinalJWT throws, tokenize returns a Failure and sends jwt and verify failed analytics`() =
        runTest(testDispatcher) {
        val cardinalClient = MockkCardinalClientBuilder().build()
        val braintreeClient = MockkBraintreeClientBuilder().build()

        val validateResponse = mockk<ValidateResponse> {
            every { actionCode } returns CardinalActionCode.SUCCESS
        }

        val exceptionMessage = "error"
        val exception = Exception(exceptionMessage)

        coEvery {
            threeDSecureAPI.authenticateCardinalJWT(any(), any())
        } throws exception

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )

        val paymentAuthResult = ThreeDSecurePaymentAuthResult("jwt", validateResponse, threeDSecureParams, null)
        sut.tokenize(paymentAuthResult, threeDSecureTokenizeCallback)
        advanceUntilIdle()

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

    @Test
    fun `when the three d secure API throws a CancellationException, createPaymentAuthRequest does not invoke the callback`() =
    runTest(testDispatcher) {
        val cardinalClient = MockkCardinalClientBuilder()
            .successReferenceId("fake-df")
            .build()

        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(threeDSecureEnabledConfig)
            .build()

        coEvery {
            threeDSecureAPI.performLookup(any(), any())
        } throws kotlin.coroutines.cancellation.CancellationException("cancelled")

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )
        sut.createPaymentAuthRequest(activity, basicRequest, paymentAuthRequestCallback)
        advanceUntilIdle()

        verify(exactly = 0) { paymentAuthRequestCallback.onThreeDSecurePaymentAuthRequest(any()) }
    }

    @Test
    fun `when the braintree client throws a CancellationException getting configuration, prepareLookup does not invoke the callback`() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationError(kotlin.coroutines.cancellation.CancellationException("cancelled"))
            .build()

        val cardinalClient = MockkCardinalClientBuilder()
            .successReferenceId("fake-df")
            .build()

        val sut = ThreeDSecureClient(
            braintreeClient,
            cardinalClient,
            threeDSecureAPI,
            merchantRepository,
            dispatcher = testDispatcher,
            coroutineScope = this
        )

        val callback = mockk<ThreeDSecurePrepareLookupCallback>(relaxed = true)
        sut.prepareLookup(activity, basicRequest, callback)
        advanceUntilIdle()

        verify(exactly = 0) { callback.onPrepareLookupResult(any()) }
    }
}
