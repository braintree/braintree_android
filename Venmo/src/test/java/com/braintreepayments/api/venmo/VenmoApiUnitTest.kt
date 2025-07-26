package com.braintreepayments.api.venmo

import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.ApiClient
import com.braintreepayments.api.core.BuildConfig
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkApiClientBuilder
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class VenmoApiUnitTest {

    private lateinit var braintreeClient: BraintreeClient
    private lateinit var apiClient: ApiClient
    private lateinit var analyticsParamRepository: AnalyticsParamRepository

    private val merchantRepository: MerchantRepository = mockk(relaxed = true)

    @Before
    fun beforeEach() {
        braintreeClient = mockk<BraintreeClient>(relaxed = true)
        apiClient = mockk<ApiClient>(relaxed = true)
        analyticsParamRepository = mockk<AnalyticsParamRepository>(relaxed = true)

        every { analyticsParamRepository.sessionId } returns "session-id"
    }

    @Test
    fun createPaymentContext_createsPaymentContextViaGraphQL() {
        val sut = VenmoApi(braintreeClient, apiClient, analyticsParamRepository, merchantRepository)
        val lineItems = ArrayList<VenmoLineItem>()
        lineItems.add(VenmoLineItem(VenmoLineItemKind.DEBIT, "Some Item", 1, "1"))

        val request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE,
            profileId = "sample-venmo-merchant",
            shouldVault = false,
            displayName = "display-name",
            collectCustomerBillingAddress = true,
            collectCustomerShippingAddress = true,
            totalAmount = "100",
            subTotalAmount = "90",
            taxAmount = "9.00",
            shippingAmount = "1",
            lineItems = lineItems
        )

        sut.createPaymentContext(request, request.profileId, mockk(relaxed = true))

        val captor = slot<JSONObject>()
        verify { braintreeClient.sendGraphQLPOST(capture(captor), any()) }

        val graphQLJSON = captor.captured

        val variables = graphQLJSON.getJSONObject("variables")
        val input = variables.getJSONObject("input")
        assertEquals("SINGLE_USE", input.getString("paymentMethodUsage"))
        assertEquals("sample-venmo-merchant", input.getString("merchantProfileId"))
        assertEquals("MOBILE_APP", input.getString("customerClient"))
        assertEquals("CONTINUE", input.getString("intent"))
        assertEquals("display-name", input.getString("displayName"))

        val metadata = graphQLJSON.getJSONObject("clientSdkMetadata")
        assertEquals(BuildConfig.VERSION_NAME, metadata.getString("version"))
        assertEquals("android", metadata.getString("platform"))
        assertEquals("session-id", metadata.getString("sessionId"))

        val paysheetDetails = input.getJSONObject("paysheetDetails")
        assertEquals("true", paysheetDetails.getString("collectCustomerBillingAddress"))
        assertEquals("true", paysheetDetails.getString("collectCustomerShippingAddress"))
        val transactionDetails = paysheetDetails.getJSONObject("transactionDetails")
        assertEquals("1", transactionDetails.getString("shippingAmount"))
        assertEquals("9.00", transactionDetails.getString("taxAmount"))
        assertEquals("90", transactionDetails.getString("subTotalAmount"))
        assertEquals("100", transactionDetails.getString("totalAmount"))
        assertFalse(transactionDetails.has("discountAmount"))

        val expectedLineItems = JSONArray().put(request.lineItems!![0].apply { unitTaxAmount = "0" }.toJson())
        assertEquals(expectedLineItems.toString(), transactionDetails.getString("lineItems"))
    }

    @Test
    fun createPaymentContext_whenTransactionAmountOptionsMissing() {
        val sut = VenmoApi(braintreeClient, apiClient, analyticsParamRepository, merchantRepository)

        val request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE,
            profileId = "sample-venmo-merchant",
            shouldVault = false,
            collectCustomerBillingAddress = true
        )

        sut.createPaymentContext(request, request.profileId, mockk(relaxed = true))

        val captor = slot<JSONObject>()
        verify { braintreeClient.sendGraphQLPOST(capture(captor), any()) }

        val graphQLJSON = captor.captured

        val variables = graphQLJSON.getJSONObject("variables")
        val input = variables.getJSONObject("input")
        assertEquals("SINGLE_USE", input.getString("paymentMethodUsage"))
        assertEquals("sample-venmo-merchant", input.getString("merchantProfileId"))
        assertEquals("MOBILE_APP", input.getString("customerClient"))
        assertEquals("CONTINUE", input.getString("intent"))
        val paysheetDetails = input.getJSONObject("paysheetDetails")
        assertEquals("true", paysheetDetails.getString("collectCustomerBillingAddress"))
        assertFalse(paysheetDetails.has("transactionDetails"))
        assertFalse(paysheetDetails.has("lineItems"))
    }

    @Test
    fun createPaymentContext_whenGraphQLPostSuccess_includesPaymentContextID_callsBackNull() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPostSuccessfulResponse(Fixtures.VENMO_GRAPHQL_CREATE_PAYMENT_METHOD_CONTEXT_RESPONSE)
            .build()
        val sut = VenmoApi(braintreeClient, apiClient, analyticsParamRepository, merchantRepository)

        val request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE,
            profileId = "sample-venmo-merchant"
        )

        val callback = mockk<VenmoApiCallback>(relaxed = true)
        sut.createPaymentContext(request, request.profileId, callback)

        verify { callback.onResult(any(), isNull()) }
    }

    @Test
    fun createPaymentContext_whenGraphQLPostSuccess_missingPaymentContextID_callsBackError() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPostSuccessfulResponse(
                Fixtures.VENMO_GRAPHQL_CREATE_PAYMENT_METHOD_RESPONSE_WITHOUT_PAYMENT_CONTEXT_ID
            )
            .build()
        val sut = VenmoApi(braintreeClient, apiClient, analyticsParamRepository, merchantRepository)

        val request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE,
            profileId = "sample-venmo-merchant"
        )

        val callback = mockk<VenmoApiCallback>(relaxed = true)
        sut.createPaymentContext(request, request.profileId, callback)

        val captor = slot<Exception>()
        verify { callback.onResult(null, capture(captor)) }

        val error = captor.captured
        assertTrue(error is BraintreeException)
        assertEquals("Failed to fetch a Venmo paymentContextId while constructing the requestURL.", error.message)
    }

    @Test
    fun createPaymentContext_whenGraphQLPostError_forwardsErrorToCallback() {
        val error = Exception("error")
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPostErrorResponse(error)
            .build()
        val sut = VenmoApi(braintreeClient, apiClient, analyticsParamRepository, merchantRepository)

        val request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE,
            profileId = "sample-venmo-merchant"
        )

        val callback = mockk<VenmoApiCallback>(relaxed = true)
        sut.createPaymentContext(request, request.profileId, callback)

        verify { callback.onResult(null, error) }
    }

    @Test
    fun createPaymentContext_withTotalAmountAndSetsFinalAmountToTrue() {
        val sut = VenmoApi(braintreeClient, apiClient, analyticsParamRepository, merchantRepository)

        val request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE,
            profileId = "sample-venmo-merchant",
            isFinalAmount = true,
            totalAmount = "5.99"
        )

        sut.createPaymentContext(request, request.profileId, mockk(relaxed = true))

        val captor = slot<JSONObject>()
        verify { braintreeClient.sendGraphQLPOST(capture(captor), any()) }

        val graphQLJSON = captor.captured

        val variables = graphQLJSON.getJSONObject("variables")
        val input = variables.getJSONObject("input")
        assertEquals("SINGLE_USE", input.getString("paymentMethodUsage"))
        assertEquals(true, input.getBoolean("isFinalAmount"))

        val paysheetDetails = input.getJSONObject("paysheetDetails")
        val transactionDetails = paysheetDetails.getJSONObject("transactionDetails")
        assertEquals("5.99", transactionDetails.getString("totalAmount"))
    }

    @Test
    fun createPaymentContext_withTotalAmountAndSetsFinalAmountToFalse() {
        val sut = VenmoApi(braintreeClient, apiClient, analyticsParamRepository, merchantRepository)

        val request = VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE).apply {
            profileId = "sample-venmo-merchant"
            isFinalAmount = false
            totalAmount = "5.99"
        }

        sut.createPaymentContext(request, request.profileId, mockk(relaxed = true))

        val captor = slot<JSONObject>()
        verify { braintreeClient.sendGraphQLPOST(capture(captor), any()) }

        val graphQLJSON = captor.captured

        val variables = graphQLJSON.getJSONObject("variables")
        val input = variables.getJSONObject("input")
        assertEquals("SINGLE_USE", input.getString("paymentMethodUsage"))
        assertEquals(false, input.getBoolean("isFinalAmount"))

        val paysheetDetails = input.getJSONObject("paysheetDetails")
        val transactionDetails = paysheetDetails.getJSONObject("transactionDetails")
        assertEquals("5.99", transactionDetails.getString("totalAmount"))
    }

    @Test
    fun createNonceFromPaymentContext_queriesGraphQLPaymentContext() {
        val sut = VenmoApi(braintreeClient, apiClient, analyticsParamRepository, merchantRepository)
        sut.createNonceFromPaymentContext("payment-context-id", mockk(relaxed = true))

        val captor = slot<JSONObject>()
        verify { braintreeClient.sendGraphQLPOST(capture(captor), any()) }

        val jsonPayload = captor.captured
        assertEquals("payment-context-id", jsonPayload.getJSONObject("variables").get("id"))
    }

    @Test
    fun createNonceFromPaymentContext_whenGraphQLPostSuccess_forwardsNonceToCallback() {
        val graphQLResponse = Fixtures.VENMO_GRAPHQL_GET_PAYMENT_CONTEXT_RESPONSE
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPostSuccessfulResponse(graphQLResponse)
            .build()

        val sut = VenmoApi(braintreeClient, apiClient, analyticsParamRepository, merchantRepository)

        val callback = mockk<VenmoInternalCallback>(relaxed = true)
        sut.createNonceFromPaymentContext("payment-context-id", callback)

        val captor = slot<VenmoAccountNonce>()
        verify { callback.onResult(capture(captor), isNull()) }
        assertEquals("@somebody", captor.captured.username)
    }

    @Test
    fun createNonceFromPaymentContext_whenGraphQLPostResponseMalformed_callsBackError() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPostSuccessfulResponse("not-json")
            .build()

        val sut = VenmoApi(braintreeClient, apiClient, analyticsParamRepository, merchantRepository)

        val callback = mockk<VenmoInternalCallback>(relaxed = true)
        sut.createNonceFromPaymentContext("payment-context-id", callback)

        val captor = slot<Exception>()
        verify { callback.onResult(null, capture(captor)) }
        assertTrue(captor.captured is JSONException)
    }

    @Test
    fun createNonceFromPaymentContext_whenGraphQLPostError_forwardsErrorToCallback() {
        val error = Exception("error")
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPostErrorResponse(error)
            .build()

        val sut = VenmoApi(braintreeClient, apiClient, analyticsParamRepository, merchantRepository)

        val callback = mockk<VenmoInternalCallback>(relaxed = true)
        sut.createNonceFromPaymentContext("payment-context-id", callback)

        verify { callback.onResult(null, error) }
    }

    @Test
    fun vaultVenmoAccountNonce_performsVaultRequest() {
        val sut = VenmoApi(braintreeClient, apiClient, analyticsParamRepository, merchantRepository)
        sut.vaultVenmoAccountNonce("nonce", mockk(relaxed = true))

        val captor = slot<VenmoAccount>()
        verify { apiClient.tokenizeREST(capture(captor), any()) }

        val venmoAccount = captor.captured
        val venmoJSON = venmoAccount.buildJSON()
        assertEquals("nonce", venmoJSON.getJSONObject("venmoAccount").getString("nonce"))
    }

    @Test
    fun vaultVenmoAccountNonce_tokenizeRESTSuccess_callsBackNonce() {
        val apiClient = MockkApiClientBuilder()
            .tokenizeRESTSuccess(JSONObject(Fixtures.VENMO_PAYMENT_METHOD_CONTEXT_WITH_NULL_PAYER_INFO_JSON))
            .build()
        val sut = VenmoApi(braintreeClient, apiClient, analyticsParamRepository, merchantRepository)

        val callback = mockk<VenmoInternalCallback>(relaxed = true)
        sut.vaultVenmoAccountNonce("nonce", callback)

        val captor = slot<VenmoAccountNonce>()
        verify { callback.onResult(capture(captor), isNull()) }

        val nonce = captor.captured
        assertEquals("@sampleuser", nonce.username)
    }

    @Test
    fun vaultVenmoAccountNonce_tokenizeRESTError_forwardsErrorToCallback() {
        val error = Exception("error")
        val apiClient = MockkApiClientBuilder()
            .tokenizeRESTError(error)
            .build()
        val sut = VenmoApi(braintreeClient, apiClient, analyticsParamRepository, merchantRepository)

        val callback = mockk<VenmoInternalCallback>(relaxed = true)
        sut.vaultVenmoAccountNonce("nonce", callback)

        verify { callback.onResult(null, error) }
    }
}
