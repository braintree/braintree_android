package com.braintreepayments.api.sepadirectdebit

import com.braintreepayments.api.core.PostalAddress
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SEPADirectDebitApiUnitTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var request: SEPADirectDebitRequest
    private lateinit var billingAddress: PostalAddress
    private lateinit var returnURL: String

    @Before
    fun beforeEach() {
        billingAddress = PostalAddress(
            streetAddress = "Kantstraße 70",
            extendedAddress = "#170",
            locality = "Freistaat Sachsen",
            region = "Annaberg-buchholz",
            postalCode = "09456",
            countryCodeAlpha2 = "FR"
        )
        request = SEPADirectDebitRequest(
            accountHolderName = "John Doe",
            iban = "FR7618106000321234566666610",
            customerId = "a-customer-id",
            mandateType = SEPADirectDebitMandateType.RECURRENT,
            merchantAccountId = "a_merchant_account_id",
            locale = "fr-FR",
            billingAddress = billingAddress
        )

        returnURL = "com.example"
    }

    @Test
    fun `creates a Mandate on successful HTTP response and calls CreateMandateResult`() =
        runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .returnUrlScheme("sample-scheme")
            .sendPostSuccessfulResponse(Fixtures.SEPA_DEBIT_CREATE_MANDATE_RESPONSE)
            .build()

        val sut = SEPADirectDebitApi(braintreeClient)

        val result = sut.createMandate(request, returnURL)

        assertEquals("6610", result.ibanLastFour)
        assertEquals(
            "https://api.test19.stage.paypal.com/directdebit/mandate/authorize?" +
                    "cart_id=1JH42426EL748934W\u0026" +
                    "auth_code=" +
                    "C21_A.AAdcUj4loKRxLtfw336KxbGY7dA7UsLJQTpZU3cE2h49e" +
                    "KkhN1OjFcLxxxzOGVzRiwOzGLlS_cS2BU4ZLKjMnR6lZSG2iQ",
            result.approvalUrl
        )
        assertEquals("QkEtWDZDQkpCUU5TWENDVw", result.bankReferenceToken)
        assertEquals("a-customer-id", result.customerId)
        assertEquals(SEPADirectDebitMandateType.RECURRENT, result.mandateType)
    }

    @Test
    fun `creates a Mandate on invalid response JSON and returns an error on MandateCallback call`() =
        runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .returnUrlScheme("sample-scheme")
            .sendPostSuccessfulResponse("not-json")
            .build()

        val sut = SEPADirectDebitApi(braintreeClient)
        try {
            sut.createMandate(request, returnURL)
            fail("Expected createMandate to throw an exception due to invalid JSON response")
        } catch (e: Exception) {
            assertNotNull(e)
            assertTrue(e is JSONException)
        }
    }

    @Test
    fun `creates a Mandate on HTTPResponseError and returns an error on MandateCallback call`() =
        runTest(testDispatcher) {
        val exception = IOException("http-error")
        val braintreeClient = MockkBraintreeClientBuilder()
            .returnUrlScheme("sample-scheme")
            .sendPostErrorResponse(exception)
            .build()

        val sut = SEPADirectDebitApi(braintreeClient)
        try {
            sut.createMandate(request, returnURL)
            fail("Expected createMandate to throw an exception due to HTTP error")
        } catch (e: Exception) {
            assertNotNull(e)
            assertEquals(exception, e)
        }
    }

    @Test
    fun `tokenizes on successful HTTP response and calls back SEPADirectDebitNonce`() =
        runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .returnUrlScheme("sample-scheme")
            .sendPostSuccessfulResponse(Fixtures.SEPA_DEBIT_TOKENIZE_RESPONSE)
            .build()

        val sut = SEPADirectDebitApi(braintreeClient)

        val result = sut.tokenize(
            ibanLastFour = "1234",
            customerId = "a-customer-id",
            bankReferenceToken = "a-bank-reference-token",
            mandateType = "ONE_OFF"
        )

        assertEquals("1234", result.ibanLastFour)
        assertEquals("a-customer-id", result.customerId)
        assertEquals(SEPADirectDebitMandateType.ONE_OFF, result.mandateType)
    }

    @Test
    fun `tokenizes on successful HTTP response with invalid JSON and calls back JSON exception`() =
        runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .returnUrlScheme("sample-scheme")
            .sendPostSuccessfulResponse("not-json")
            .build()

        val sut = SEPADirectDebitApi(braintreeClient)
        try {
            sut.tokenize(
                ibanLastFour = "1234",
                customerId = "a-customer-id",
                bankReferenceToken = "a-bank-reference-token",
                mandateType = "ONE_OFF")
            fail("Expected tokenize to throw an exception due to invalid JSON response")
        } catch (e: Exception) {
            assertNotNull(e)
            assertTrue(e is JSONException)
        }
    }

    @Test
    fun `tokenizes on HTTP error and calls back an error`() = runTest(testDispatcher) {
        val error = IOException("http error")
        val braintreeClient = MockkBraintreeClientBuilder()
            .returnUrlScheme("sample-scheme")
            .sendPostErrorResponse(error)
            .build()

        val sut = SEPADirectDebitApi(braintreeClient)
        try {
            sut.tokenize(
                ibanLastFour = "1234",
                customerId = "a-customer-id",
                bankReferenceToken = "a-bank-reference-token",
                mandateType = "ONE_OFF"
            )
            fail("Expected tokenize to throw an exception due to HTTP error")
        } catch (e: Exception) {
            assertNotNull(e)
            assertEquals(error, e)
        }
    }

    @Test
    fun `creates a Mandate and properly formats POST body`() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .returnUrlScheme("com.example")
            .build()

        val sut = SEPADirectDebitApi(braintreeClient)

        val slotString = slot<String>()
        coEvery {
            braintreeClient.sendPOST(url = eq("/v1/sepa_debit"), data = capture(slotString))
        } returns Fixtures.SEPA_DEBIT_CREATE_MANDATE_RESPONSE

        sut.createMandate(request, returnURL)

        coVerify {
            braintreeClient.sendPOST(url = eq("/v1/sepa_debit"), data = capture(slotString))
        }

        val result = slotString.captured
        val json = JSONObject(result)
        assertEquals("com.example://sepa/cancel", json.getString("cancel_url"))
        assertEquals("com.example://sepa/success", json.getString("return_url"))
        assertEquals("a_merchant_account_id", json.getString("merchant_account_id"))
        assertEquals("fr-FR", json.getString("locale"))

        val sepaJson = json.getJSONObject("sepa_debit")
        assertEquals("John Doe", sepaJson.getString("account_holder_name"))
        assertEquals("a-customer-id", sepaJson.getString("merchant_or_partner_customer_id"))
        assertEquals("FR7618106000321234566666610", sepaJson.getString("iban"))
        assertEquals("RECURRENT", sepaJson.getString("mandate_type"))

        val billingAddressJson = sepaJson.getJSONObject("billing_address")
        assertEquals("Kantstraße 70", billingAddressJson.getString("address_line_1"))
        assertEquals("#170", billingAddressJson.getString("address_line_2"))
        assertEquals("Freistaat Sachsen", billingAddressJson.getString("admin_area_1"))
        assertEquals("Annaberg-buchholz", billingAddressJson.getString("admin_area_2"))
        assertEquals("09456", billingAddressJson.getString("postal_code"))
        assertEquals("FR", billingAddressJson.getString("country_code"))
    }
}
