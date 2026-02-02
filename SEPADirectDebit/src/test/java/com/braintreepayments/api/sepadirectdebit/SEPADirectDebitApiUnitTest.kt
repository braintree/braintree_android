package com.braintreepayments.api.sepadirectdebit

import com.braintreepayments.api.core.PostalAddress
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SEPADirectDebitApiUnitTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var createMandateCallback: CreateMandateCallback
    private lateinit var sepaDirectDebitTokenizeCallback: SEPADirectDebitInternalTokenizeCallback
    private lateinit var request: SEPADirectDebitRequest
    private lateinit var billingAddress: PostalAddress
    private lateinit var returnURL: String

    @Before
    fun beforeEach() {
        createMandateCallback = mockk<CreateMandateCallback>(relaxed = true)
        sepaDirectDebitTokenizeCallback = mockk<SEPADirectDebitInternalTokenizeCallback>(relaxed = true)
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
    fun `creates a Mandate on successful HTTP response and calls CreateMandateResult`() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .returnUrlScheme("sample-scheme")
            .sendPostSuccessfulResponse(Fixtures.SEPA_DEBIT_CREATE_MANDATE_RESPONSE)
            .build()

        val sut = SEPADirectDebitApi(
            braintreeClient,
            dispatcher = testDispatcher,
            coroutineScope = TestScope(testDispatcher)
        )
        sut.createMandate(request, returnURL, createMandateCallback)
        advanceUntilIdle()
        val createMandateSlot = slot<CreateMandateResult>()
        verify { createMandateCallback.onResult(capture(createMandateSlot), isNull()) }

        val result = createMandateSlot.captured

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

        val sut = SEPADirectDebitApi(
            braintreeClient,
            dispatcher = testDispatcher,
            coroutineScope = TestScope(testDispatcher)
        )
        sut.createMandate(request, returnURL, createMandateCallback)
        advanceUntilIdle()
        val exceptionSlot = slot<Exception>()
        verify { createMandateCallback.onResult(isNull(), capture(exceptionSlot)) }

        val error = exceptionSlot.captured

        assertNotNull(error)
        assertTrue(error is JSONException)
    }

    @Test
    fun `creates a Mandate on HTTPResponseError and returns an error on MandateCallback call`() =
        runTest(testDispatcher) {
        val exception = IOException("http-error")
        val braintreeClient = MockkBraintreeClientBuilder()
            .returnUrlScheme("sample-scheme")
            .sendPostErrorResponse(exception)
            .build()

        val sut = SEPADirectDebitApi(
            braintreeClient,
            dispatcher = testDispatcher,
            coroutineScope = TestScope(testDispatcher)
        )
        sut.createMandate(request, returnURL, createMandateCallback)
        advanceUntilIdle()
        verify { createMandateCallback.onResult(null, eq(exception)) }
    }

    @Test
    fun `tokenizes on successful HTTP response and calls back SEPADirectDebitNonce`() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .returnUrlScheme("sample-scheme")
            .sendPostSuccessfulResponse(Fixtures.SEPA_DEBIT_TOKENIZE_RESPONSE)
            .build()

        val sut = SEPADirectDebitApi(
            braintreeClient,
            dispatcher = testDispatcher,
            coroutineScope = TestScope(testDispatcher)
        )
        sut.tokenize(
            ibanLastFour = "1234",
            customerId = "a-customer-id",
            bankReferenceToken = "a-bank-reference-token",
            mandateType = "ONE_OFF",
            sepaDirectDebitTokenizeCallback
        )
        advanceUntilIdle()
        val sepaDirectDebitNonceSlot = slot<SEPADirectDebitNonce>()
        verify { sepaDirectDebitTokenizeCallback.onResult(capture(sepaDirectDebitNonceSlot), null) }

        val result = sepaDirectDebitNonceSlot.captured
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

        val sut = SEPADirectDebitApi(
            braintreeClient,
            dispatcher = testDispatcher,
            coroutineScope = TestScope(testDispatcher)
        )
        sut.tokenize(
            ibanLastFour = "1234",
            customerId = "a-customer-id",
            bankReferenceToken = "a-bank-reference-token",
            mandateType = "ONE_OFF",
            sepaDirectDebitTokenizeCallback
        )
        advanceUntilIdle()
        val exceptionSlot = slot<Exception>()
        verify { sepaDirectDebitTokenizeCallback.onResult(null, capture(exceptionSlot)) }

        val exception = exceptionSlot.captured
        assertTrue(exception is JSONException)
    }

    @Test
    fun `tokenizes on HTTP error and calls back an error`() = runTest(testDispatcher) {
        val error = IOException("http error")
        val braintreeClient = MockkBraintreeClientBuilder()
            .returnUrlScheme("sample-scheme")
            .sendPostErrorResponse(error)
            .build()

        val sut = SEPADirectDebitApi(
            braintreeClient,
            dispatcher = testDispatcher,
            coroutineScope = TestScope(testDispatcher)
        )
        sut.tokenize(
            ibanLastFour = "1234",
            customerId = "a-customer-id",
            bankReferenceToken = "a-bank-reference-token",
            mandateType = "ONE_OFF",
            sepaDirectDebitTokenizeCallback
        )
        advanceUntilIdle()
        verify { sepaDirectDebitTokenizeCallback.onResult(null, error) }
    }

    @Test
    fun `creates a Mandate and properly formats POST body`() = runTest(testDispatcher) {
        val braintreeClient = MockkBraintreeClientBuilder()
            .returnUrlScheme("com.example")
            .build()

        val sut = SEPADirectDebitApi(
            braintreeClient,
            dispatcher = testDispatcher,
            coroutineScope = TestScope(testDispatcher)
        )

        val slotString = slot<String>()
        coEvery {
            braintreeClient.sendPOST(url = eq("/v1/sepa_debit"), data = capture(slotString))
        } returns "{}"

        sut.createMandate(request, returnURL, createMandateCallback)
        advanceUntilIdle()
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
