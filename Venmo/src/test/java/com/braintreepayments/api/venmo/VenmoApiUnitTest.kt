package com.braintreepayments.api.venmo

import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.ApiClient
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.BuildConfig
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkApiClientBuilder
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Suppress("MaxLineLength")
class VenmoApiUnitTest {

    private lateinit var braintreeClient: BraintreeClient
    private lateinit var apiClient: ApiClient
    private lateinit var analyticsParamRepository: AnalyticsParamRepository

    private val merchantRepository: MerchantRepository = mockk(relaxed = true)

    @Before
    fun beforeEach() {
        braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPostSuccessfulResponse(
                Fixtures.VENMO_GRAPHQL_CREATE_PAYMENT_METHOD_CONTEXT_RESPONSE)
            .build()
        apiClient = mockk<ApiClient>(relaxed = true)
        analyticsParamRepository = mockk<AnalyticsParamRepository>(relaxed = true)
        every { analyticsParamRepository.sessionId } returns "session-id"
    }

    @Test
    fun `createPaymentContext sends a graphQL request with the expected variables and metadata`() = runTest {
        val sut = VenmoApi(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            merchantRepository
        )
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

        sut.createPaymentContext(request, request.profileId)

        val captor = slot<JSONObject>()
        coVerify { braintreeClient.sendGraphQLPOST(capture(captor)) }

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
    fun `when transaction amount options are missing, createPaymentContext omits transactionDetails and lineItems`() = runTest {
        val sut = VenmoApi(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            merchantRepository
        )

        val request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE,
            profileId = "sample-venmo-merchant",
            shouldVault = false,
            collectCustomerBillingAddress = true
        )

        sut.createPaymentContext(request, request.profileId)

        val captor = slot<JSONObject>()
        coVerify { braintreeClient.sendGraphQLPOST(capture(captor)) }

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
    fun `when graphQL post succeeds, createPaymentContext returns a non-null payment context response`() = runTest {
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPostSuccessfulResponse(Fixtures.VENMO_GRAPHQL_CREATE_PAYMENT_METHOD_CONTEXT_RESPONSE)
            .build()

        val sut = VenmoApi(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            merchantRepository
        )

        val request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE,
            profileId = "sample-venmo-merchant"
        )

        val contextPaymentResponse = sut.createPaymentContext(request, request.profileId)
        assertNotNull(contextPaymentResponse)
    }

    @Test
    fun `when graphQL response is missing the payment context id, createPaymentContext throws an exception`() = runTest {
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPostSuccessfulResponse(
                Fixtures.VENMO_GRAPHQL_CREATE_PAYMENT_METHOD_RESPONSE_WITHOUT_PAYMENT_CONTEXT_ID
            )
            .build()

        val sut = VenmoApi(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            merchantRepository
        )

        val request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE,
            profileId = "sample-venmo-merchant"
        )

        val error = assertFailsWith<BraintreeException> {
            sut.createPaymentContext(request, request.profileId)
        }
        assertEquals("Failed to fetch a Venmo paymentContextId while constructing the requestURL.", error.message)
    }

    @Test
    fun `when graphQL post fails, createPaymentContext propagates the error`() = runTest {
        val error = IOException("error")
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPostErrorResponse(error)
            .build()

        val sut = VenmoApi(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            merchantRepository
        )

        val request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE,
            profileId = "sample-venmo-merchant"
        )
        assertFailsWith<IOException> {
            sut.createPaymentContext(request, request.profileId)
        }
    }

    @Test
    fun `when isFinalAmount is true, createPaymentContext sends isFinalAmount true and the total amount`() = runTest {
        val sut = VenmoApi(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            merchantRepository
        )

        val request = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE,
            profileId = "sample-venmo-merchant",
            isFinalAmount = true,
            totalAmount = "5.99"
        )

        sut.createPaymentContext(request, request.profileId)

        val captor = slot<JSONObject>()
        coVerify { braintreeClient.sendGraphQLPOST(capture(captor)) }

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
    fun `when isFinalAmount is false, createPaymentContext sends isFinalAmount false and the total amount`() = runTest {
        val sut = VenmoApi(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            merchantRepository
        )

        val request = VenmoRequest(VenmoPaymentMethodUsage.SINGLE_USE).apply {
            profileId = "sample-venmo-merchant"
            isFinalAmount = false
            totalAmount = "5.99"
        }

        sut.createPaymentContext(request, request.profileId)
        val captor = slot<JSONObject>()
        coVerify { braintreeClient.sendGraphQLPOST(capture(captor)) }

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
    fun `createNonceFromPaymentContext sends the payment context id as a graphQL query variable`() = runTest {
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPostSuccessfulResponse(Fixtures.VENMO_GRAPHQL_GET_PAYMENT_CONTEXT_RESPONSE)
            .build()
        val sut = VenmoApi(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            merchantRepository
        )
        sut.createNonceFromPaymentContext("payment-context-id")

        val captor = slot<JSONObject>()
        coVerify { braintreeClient.sendGraphQLPOST(capture(captor)) }

        val jsonPayload = captor.captured
        assertEquals("payment-context-id", jsonPayload.getJSONObject("variables").get("id"))
    }

    @Test
    fun `when graphQL post succeeds, createNonceFromPaymentContext returns a nonce with the expected username`() = runTest {
        val graphQLResponse = Fixtures.VENMO_GRAPHQL_GET_PAYMENT_CONTEXT_RESPONSE
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPostSuccessfulResponse(graphQLResponse)
            .build()

        val sut = VenmoApi(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            merchantRepository
        )

        val nonce = sut.createNonceFromPaymentContext("payment-context-id")

        assertEquals("@somebody", nonce.username)
    }

    @Test
    fun `when graphQL response is malformed json, createNonceFromPaymentContext throws a JSONException`() = runTest {
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPostSuccessfulResponse("not-json")
            .build()

        val sut = VenmoApi(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            merchantRepository
        )

        assertFailsWith<JSONException> {
            sut.createNonceFromPaymentContext("payment-context-id")
        }
    }

    @Test
    fun `when graphQL post fails, createNonceFromPaymentContext propagates the error`() = runTest {
        val error = IOException("error")
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendGraphQLPostErrorResponse(error)
            .build()

        val sut = VenmoApi(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            merchantRepository
        )

        assertFailsWith<IOException> {
            sut.createNonceFromPaymentContext("payment-context-id")
        }
    }

    @Test
    fun `vaultVenmoAccountNonce sends a tokenize request containing the nonce`() = runTest {
        val sut = VenmoApi(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            merchantRepository
        )
        sut.vaultVenmoAccountNonce("nonce")

        val captor = slot<VenmoAccount>()
        coVerify { apiClient.tokenizeREST(capture(captor)) }

        val venmoAccount = captor.captured
        val venmoJSON = venmoAccount.buildJSON()
        assertEquals("nonce", venmoJSON.getJSONObject("venmoAccount").getString("nonce"))
    }

    @Test
    fun `when tokenizeREST succeeds, vaultVenmoAccountNonce returns the resulting nonce`() = runTest {
        val apiClient = MockkApiClientBuilder()
            .tokenizeRESTSuccess(JSONObject(Fixtures.VENMO_PAYMENT_METHOD_CONTEXT_WITH_NULL_PAYER_INFO_JSON))
            .build()
        val sut = VenmoApi(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            merchantRepository
        )

        val nonce = sut.vaultVenmoAccountNonce("nonce")
        assertEquals("@sampleuser", nonce.username)
    }

    @Test
    fun `when tokenizeREST fails, vaultVenmoAccountNonce propagates the error`() = runTest {
        val error = Exception("error")
        val apiClient = MockkApiClientBuilder()
            .tokenizeRESTError(error)
            .build()
        val sut = VenmoApi(
            braintreeClient,
            apiClient,
            analyticsParamRepository,
            merchantRepository
        )

        assertFailsWith<Exception> {
            sut.vaultVenmoAccountNonce("nonce")
        }
    }
}
