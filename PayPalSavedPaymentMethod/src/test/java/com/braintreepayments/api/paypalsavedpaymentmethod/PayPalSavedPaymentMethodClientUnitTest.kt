@file:OptIn(ExperimentalBetaApi::class)

package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.core.GraphQLConstants
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.FixturesHelper
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import com.braintreepayments.api.testutils.TestConfigurationBuilder
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class PayPalSavedPaymentMethodClientUnitTest {

    private val clientTokenRepository =
        repositoryWith(FixturesHelper.base64Encode(Fixtures.CLIENT_TOKEN_WITH_PAYMENT_METHOD_ID_JWT))
    private val clientTokenWithoutJwtRepository = repositoryWith(Fixtures.BASE64_CLIENT_TOKEN)
    private val tokenizationKeyRepository = repositoryWith(Fixtures.TOKENIZATION_KEY)

    @Test
    fun `fetchPaymentMethod for a buyer default billing agreement sends the paymentMethodIdJwt`() = runTest {
        val braintreeClient = graphQLClient(INSTRUMENT_RESPONSE)
        val sut = PayPalSavedPaymentMethodClient(braintreeClient, clientTokenRepository)

        val result = sut.fetchPaymentMethod(
            fundingInstrumentType = PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT,
            merchantAccountId = "merchant-account-1"
        )

        val input = capturedGraphQLInput(braintreeClient)
        assertEquals("STICKY_FI", input.getString("fundingInstrumentType"))
        assertEquals("payment_method_id_jwt", input.getString("paymentMethodIdJwt"))
        assertEquals("merchant-account-1", input.getString("merchantAccountId"))
        assertFalse(input.has("orderId"))
        assertEquals("CREDIT UNION 1", result.paymentMethods.first().label)
        assertNull(result.payer)
    }

    @Test
    fun `fetchPaymentMethod for a buyer default billing agreement ignores an orderId`() = runTest {
        val braintreeClient = graphQLClient(INSTRUMENT_RESPONSE)
        val sut = PayPalSavedPaymentMethodClient(braintreeClient, clientTokenRepository)

        sut.fetchPaymentMethod(
            fundingInstrumentType = PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT,
            orderId = "order-456"
        )

        assertFalse(capturedGraphQLInput(braintreeClient).has("orderId"))
    }

    @Test
    fun `fetchPaymentMethod for a buyer updated billing agreement sends the orderId`() = runTest {
        val braintreeClient = graphQLClient(PAYER_RESPONSE)
        val sut = PayPalSavedPaymentMethodClient(braintreeClient, clientTokenRepository)

        val result = sut.fetchPaymentMethod(
            fundingInstrumentType = PayPalFundingInstrumentFetchType.BUYER_UPDATED_BILLING_AGREEMENT,
            orderId = "order-456"
        )

        val input = capturedGraphQLInput(braintreeClient)
        assertEquals("FI_FROM_APPROVED_CHECKOUT", input.getString("fundingInstrumentType"))
        assertEquals("order-456", input.getString("orderId"))
        assertFalse(input.has("paymentMethodIdJwt"))
        assertEquals("buyer@example.com", result.payer?.email)
        assertEquals(emptyList<PayPalSavedPaymentMethod>(), result.paymentMethods)
    }

    @Test
    fun `fetchPaymentMethod returns an empty summary when there is nothing to display`() = runTest {
        val braintreeClient = graphQLClient(
            """{ "data": { "paypalFundingInstrumentDetails": { "payer": null, "paymentMethods": [] } } }"""
        )
        val sut = PayPalSavedPaymentMethodClient(braintreeClient, clientTokenRepository)

        val result = sut.fetchPaymentMethod(PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT)

        assertEquals(emptyList<PayPalSavedPaymentMethod>(), result.paymentMethods)
        assertNull(result.payer)
    }

    @Test
    fun `fetchPaymentMethod rejects a tokenization key without sending a request`() = runTest {
        val braintreeClient = MockkBraintreeClientBuilder().build()
        val sut = PayPalSavedPaymentMethodClient(braintreeClient, tokenizationKeyRepository)

        assertFailsWith<PayPalSavedPaymentMethodException.InvalidAuthorization> {
            sut.fetchPaymentMethod(PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT)
        }
        coVerify(exactly = 0) { braintreeClient.sendGraphQLPOST(any()) }
    }

    @Test
    fun `fetchPaymentMethod throws MissingPaymentMethodIdJwt without sending a request`() = runTest {
        val braintreeClient = MockkBraintreeClientBuilder().build()
        val sut = PayPalSavedPaymentMethodClient(braintreeClient, clientTokenWithoutJwtRepository)

        assertFailsWith<PayPalSavedPaymentMethodException.MissingPaymentMethodIdJwt> {
            sut.fetchPaymentMethod(PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT)
        }
        coVerify(exactly = 0) { braintreeClient.sendGraphQLPOST(any()) }
    }

    @Test
    fun `fetchPaymentMethod throws MissingOrderId without sending a request`() = runTest {
        val braintreeClient = MockkBraintreeClientBuilder().build()
        val sut = PayPalSavedPaymentMethodClient(braintreeClient, clientTokenRepository)

        assertFailsWith<PayPalSavedPaymentMethodException.MissingOrderId> {
            sut.fetchPaymentMethod(PayPalFundingInstrumentFetchType.BUYER_UPDATED_BILLING_AGREEMENT)
        }
        coVerify(exactly = 0) { braintreeClient.sendGraphQLPOST(any()) }
    }

    @Test
    fun `fetchPaymentMethod throws EmptyBodyReturned when the response body is empty`() = runTest {
        val sut = PayPalSavedPaymentMethodClient(graphQLClient(""), clientTokenRepository)

        assertFailsWith<PayPalSavedPaymentMethodException.EmptyBodyReturned> {
            sut.fetchPaymentMethod(PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT)
        }
    }

    @Test
    fun `fetchPaymentMethod throws a BraintreeException with the message from GraphQL errors`() = runTest {
        val braintreeClient = graphQLClient(
            """
            {
              "errors": [
                {
                  "message": "PAYMENT_METHOD_NOT_FOUND",
                  "path": [ "paypalFundingInstrumentDetails" ],
                  "extensions": { "errorClass": "NOT_FOUND", "errorType": "user_error" }
                }
              ],
              "data": { "paypalFundingInstrumentDetails": null }
            }
            """
        )
        val sut = PayPalSavedPaymentMethodClient(braintreeClient, clientTokenRepository)

        val error = assertFailsWith<BraintreeException> {
            sut.fetchPaymentMethod(PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT)
        }

        assertEquals("PAYMENT_METHOD_NOT_FOUND", error.message)
    }

    @Test
    fun `fetchPaymentMethod uses the response as the message when a GraphQL error has none`() = runTest {
        val response = """{ "errors": [ {} ] }"""
        val sut = PayPalSavedPaymentMethodClient(graphQLClient(response), clientTokenRepository)

        val error = assertFailsWith<BraintreeException> {
            sut.fetchPaymentMethod(PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT)
        }

        assertEquals(response, error.message)
    }

    @Test
    fun `fetchPaymentMethod throws FailedToParseSummary without funding instrument details`() = runTest {
        val sut = PayPalSavedPaymentMethodClient(graphQLClient("{}"), clientTokenRepository)

        assertFailsWith<PayPalSavedPaymentMethodException.FailedToParseSummary> {
            sut.fetchPaymentMethod(PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT)
        }
    }

    @Test
    fun `fetchPaymentMethod throws a JSONException when the response is not valid JSON`() = runTest {
        val sut = PayPalSavedPaymentMethodClient(graphQLClient("not json"), clientTokenRepository)

        assertFailsWith<JSONException> {
            sut.fetchPaymentMethod(PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT)
        }
    }

    @Test
    fun `fetchPaymentMethod propagates the error when the request fails`() = runTest {
        val requestError = IOException("network down")
        val braintreeClient = MockkBraintreeClientBuilder().sendGraphQLPostErrorResponse(requestError).build()
        val sut = PayPalSavedPaymentMethodClient(braintreeClient, clientTokenRepository)

        val error = assertFailsWith<IOException> {
            sut.fetchPaymentMethod(PayPalFundingInstrumentFetchType.BUYER_DEFAULT_BILLING_AGREEMENT)
        }

        assertSame(requestError, error)
    }

    @Test
    fun `fetchCreditPresentmentMessages posts the request body to the sandbox URL`() = runTest {
        val braintreeClient = restClient("sandbox", MESSAGING_RESPONSE)
        val sut = PayPalSavedPaymentMethodClient(braintreeClient, clientTokenRepository)

        sut.fetchCreditPresentmentMessages(amount = "55.00", currencyCode = "USD")

        val url = slot<String>()
        val data = slot<String>()
        coVerify { braintreeClient.sendPOST(capture(url), capture(data), any()) }
        assertEquals("https://api.sandbox.paypal.com/v2/credit/fetch-presentment-messages", url.captured)

        val body = JSONObject(data.captured)
        val placement = body.getJSONArray("message_placements").getJSONObject(0)
        assertEquals("55.00", placement.getJSONObject("amount").getString("value"))
        assertEquals("USD", placement.getJSONObject("amount").getString("currency_code"))
        assertEquals(
            listOf("ALTERNATIVE_PREFIX_UPPERCASE_OR", "MESSAGE_LENGTH_COMPACT"),
            placement.getJSONArray("content_attributes").toStringList()
        )

        val flowContext = body.getJSONObject("flow_context")
        assertEquals("MOBILE_APP", flowContext.getString("channel"))
        assertEquals("EARLY_PRESENTMENT", flowContext.getString("flow_specifier"))
        assertEquals(
            listOf(
                "BRAND_BRAINTREE",
                "EXPERIENCE_ANDROID_SDK",
                "EXPERIENCE_VIEW_EDIT_FI",
                "EXPERIENCE_EXTERNAL_DIRECT"
            ),
            flowContext.getJSONArray("attributes").toStringList()
        )
    }

    @Test
    fun `fetchCreditPresentmentMessages posts to the production URL in production`() = runTest {
        val braintreeClient = restClient("production", MESSAGING_RESPONSE)
        val sut = PayPalSavedPaymentMethodClient(braintreeClient, clientTokenRepository)

        sut.fetchCreditPresentmentMessages(amount = "55.00", currencyCode = "USD")

        val url = slot<String>()
        coVerify { braintreeClient.sendPOST(capture(url), any(), any()) }
        assertEquals("https://api.paypal.com/v2/credit/fetch-presentment-messages", url.captured)
    }

    @Test
    fun `fetchCreditPresentmentMessages returns the parsed message`() = runTest {
        val sut = PayPalSavedPaymentMethodClient(restClient("sandbox", MESSAGING_RESPONSE), clientTokenRepository)

        val result = sut.fetchCreditPresentmentMessages(amount = "55.00", currencyCode = "USD")

        assertEquals("Pay in 4", result.mainItems.single().text)
    }

    @Test
    fun `fetchCreditPresentmentMessages rejects a tokenization key without sending a request`() = runTest {
        val braintreeClient = MockkBraintreeClientBuilder().build()
        val sut = PayPalSavedPaymentMethodClient(braintreeClient, tokenizationKeyRepository)

        assertFailsWith<PayPalSavedPaymentMethodException.InvalidAuthorization> {
            sut.fetchCreditPresentmentMessages(amount = "55.00", currencyCode = "USD")
        }
        coVerify(exactly = 0) { braintreeClient.sendPOST(any(), any(), any()) }
    }

    @Test
    fun `fetchCreditPresentmentMessages throws EmptyBodyReturned when the response body is empty`() = runTest {
        val sut = PayPalSavedPaymentMethodClient(restClient("sandbox", ""), clientTokenRepository)

        assertFailsWith<PayPalSavedPaymentMethodException.EmptyBodyReturned> {
            sut.fetchCreditPresentmentMessages(amount = "55.00", currencyCode = "USD")
        }
    }

    @Test
    fun `fetchCreditPresentmentMessages throws MissingPreferredMessage when there is no message to render`() = runTest {
        val sut = PayPalSavedPaymentMethodClient(restClient("sandbox", """{ "messages": [] }"""), clientTokenRepository)

        assertFailsWith<PayPalSavedPaymentMethodException.MissingPreferredMessage> {
            sut.fetchCreditPresentmentMessages(amount = "55.00", currencyCode = "USD")
        }
    }

    @Test
    fun `fetchCreditPresentmentMessages throws a JSONException when the response is not valid JSON`() = runTest {
        val sut = PayPalSavedPaymentMethodClient(restClient("sandbox", "not json"), clientTokenRepository)

        assertFailsWith<JSONException> {
            sut.fetchCreditPresentmentMessages(amount = "55.00", currencyCode = "USD")
        }
    }

    @Test
    fun `fetchCreditPresentmentMessages propagates the error when the request fails`() = runTest {
        val requestError = IOException("network down")
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(Configuration.fromJson(TestConfigurationBuilder().environment("sandbox").build()))
            .sendPostErrorResponse(requestError)
            .build()
        val sut = PayPalSavedPaymentMethodClient(braintreeClient, clientTokenRepository)

        val error = assertFailsWith<IOException> {
            sut.fetchCreditPresentmentMessages(amount = "55.00", currencyCode = "USD")
        }

        assertSame(requestError, error)
    }

    private fun repositoryWith(authorizationString: String): MerchantRepository = mockk {
        every { authorization } returns Authorization.fromString(authorizationString)
    }

    private fun graphQLClient(response: String): BraintreeClient =
        MockkBraintreeClientBuilder().sendGraphQLPostSuccessfulResponse(response).build()

    private fun restClient(environment: String, response: String): BraintreeClient =
        MockkBraintreeClientBuilder()
            .configurationSuccess(
                Configuration.fromJson(TestConfigurationBuilder().environment(environment).build())
            )
            .sendPostSuccessfulResponse(response)
            .build()

    private fun capturedGraphQLInput(braintreeClient: BraintreeClient): JSONObject {
        val body = slot<JSONObject>()
        coVerify { braintreeClient.sendGraphQLPOST(capture(body)) }
        return body.captured.getJSONObject(GraphQLConstants.Keys.VARIABLES).getJSONObject(GraphQLConstants.Keys.INPUT)
    }

    private fun JSONArray.toStringList(): List<String> = (0 until length()).map { getString(it) }

    private companion object {
        const val INSTRUMENT_RESPONSE = """
            {
              "data": {
                "paypalFundingInstrumentDetails": {
                  "payer": null,
                  "paymentMethods": [ { "type": "BANK", "label": "CREDIT UNION 1", "lastDigits": "3357" } ]
                }
              }
            }
        """

        const val PAYER_RESPONSE = """
            {
              "data": {
                "paypalFundingInstrumentDetails": {
                  "payer": { "email": "buyer@example.com", "editable": true },
                  "paymentMethods": []
                }
              }
            }
        """

        const val MESSAGING_RESPONSE = """
            {
              "messages": [
                { "preferred_message": { "content": { "main_items": [ { "type": "TEXT", "text": "Pay in 4" } ] } } }
              ]
            }
        """
    }
}
