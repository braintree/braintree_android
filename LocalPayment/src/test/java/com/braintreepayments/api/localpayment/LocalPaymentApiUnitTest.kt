package com.braintreepayments.api.localpayment

import android.net.Uri
import com.braintreepayments.api.core.IntegrationType
import com.braintreepayments.api.core.PostalAddress
import com.braintreepayments.api.sharedutils.HttpResponseCallback
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockBraintreeClientBuilder
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert

@RunWith(RobolectricTestRunner::class)
class LocalPaymentApiUnitTest {
    private var localPaymentInternalAuthRequestCallback: LocalPaymentInternalAuthRequestCallback? =
        null
    private var localPaymentInternalTokenizeCallback: LocalPaymentInternalTokenizeCallback? = null

    @Before
    fun beforeEach() {
        localPaymentInternalAuthRequestCallback = Mockito.mock(
            LocalPaymentInternalAuthRequestCallback::class.java
        )
        localPaymentInternalTokenizeCallback =
            Mockito.mock(LocalPaymentInternalTokenizeCallback::class.java)
    }

    @Test
    @Throws(JSONException::class)
    fun createPaymentMethod_sendsCorrectPostParams() {
        val braintreeClient = MockBraintreeClientBuilder()
            .returnUrlScheme("sample-scheme")
            .build()

        val sut = LocalPaymentApi(braintreeClient)
        sut.createPaymentMethod(
            idealLocalPaymentRequest,
            localPaymentInternalAuthRequestCallback!!
        )

        val expectedPath = "/v1/local_payments/create"
        val bodyCaptor = ArgumentCaptor.forClass(
            String::class.java
        )
        Mockito.verify(braintreeClient).sendPOST(
            url = ArgumentMatchers.eq(expectedPath), data = bodyCaptor.capture(),
            responseCallback = ArgumentMatchers.any(HttpResponseCallback::class.java)
        )

        val requestBody = bodyCaptor.value
        val json = JSONObject(requestBody)
        assertEquals("Doe", json.getString("lastName"))
        assertEquals("1.10", json.getString("amount"))
        assertEquals("Den Haag", json.getString("city"))
        assertEquals("2585 GJ", json.getString("postalCode"))
        assertEquals("sale", json.getString("intent"))
        assertEquals("Jon", json.getString("firstName"))
        assertEquals("639847934", json.getString("phone"))
        assertEquals("NL", json.getString("countryCode"))
        assertEquals("EUR", json.getString("currencyIsoCode"))
        assertEquals("ideal", json.getString("fundingSource"))
        assertEquals("jon@getbraintree.com", json.getString("payerEmail"))
        assertEquals("836486 of 22321 Park Lake", json.getString("line1"))
        assertEquals("Apt 2", json.getString("line2"))
        assertEquals("CA", json.getString("state"))
        assertEquals("local-merchant-account-id", json.getString("merchantAccountId"))
        assertTrue(json.getJSONObject("experienceProfile").getBoolean("noShipping"))
        assertEquals(
            "My Brand!",
            json.getJSONObject("experienceProfile").getString("brandName")
        )
        val expectedCancelUrl = Uri.parse("sample-scheme://local-payment-cancel").toString()
        val expectedReturnUrl = Uri.parse("sample-scheme://local-payment-success").toString()
        assertEquals(expectedCancelUrl, json.getString("cancelUrl"))
        assertEquals(expectedReturnUrl, json.getString("returnUrl"))
    }

    @Test
    fun createPaymentMethod_onPOSTError_forwardsErrorToCallback() {
        val error = Exception("error")
        val braintreeClient = MockBraintreeClientBuilder()
            .sendPOSTErrorResponse(error)
            .build()

        val sut = LocalPaymentApi(braintreeClient)
        sut.createPaymentMethod(
            idealLocalPaymentRequest,
            localPaymentInternalAuthRequestCallback!!
        )

        Mockito.verify(localPaymentInternalAuthRequestCallback)?.onLocalPaymentInternalAuthResult(
            ArgumentMatchers.isNull(),
            ArgumentMatchers.same(error)
        )
    }

    @Test
    fun createPaymentMethod_onJSONError_forwardsJSONErrorToCallback() {
        val braintreeClient = MockBraintreeClientBuilder()
            .sendPOSTSuccessfulResponse(Fixtures.ERROR_RESPONSE)
            .build()

        val sut = LocalPaymentApi(braintreeClient)
        sut.createPaymentMethod(
            idealLocalPaymentRequest,
            localPaymentInternalAuthRequestCallback!!
        )

        val captor = ArgumentCaptor.forClass(
            Exception::class.java
        )
        Mockito.verify(localPaymentInternalAuthRequestCallback)
            ?.onLocalPaymentInternalAuthResult(ArgumentMatchers.isNull(), captor.capture())

        assertTrue(captor.value is JSONException)
    }

    @Test
    fun createPaymentMethod_onPOSTSuccess_returnsResultWithOriginalRequestToCallback() {
        val braintreeClient = MockBraintreeClientBuilder()
            .sendPOSTSuccessfulResponse(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_CREATE_RESPONSE)
            .build()

        val sut = LocalPaymentApi(braintreeClient)
        val request = idealLocalPaymentRequest
        sut.createPaymentMethod(request, localPaymentInternalAuthRequestCallback!!)

        val captor =
            ArgumentCaptor.forClass(
                LocalPaymentAuthRequestParams::class.java
            )
        Mockito.verify(localPaymentInternalAuthRequestCallback)
            ?.onLocalPaymentInternalAuthResult(captor.capture(), ArgumentMatchers.isNull())

        val result = captor.value
        assertNotNull(result)
        assertSame(request, result.request)
        assertEquals(
            "https://checkout.paypal.com/latinum?token=payment-token",
            result.approvalUrl
        )
        assertEquals("local-payment-id-123", result.paymentId)
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_sendsCorrectPostParams() {
        val braintreeClient = MockBraintreeClientBuilder()
            .sessionId("sample-session-id")
            .integration(IntegrationType.CUSTOM)
            .build()

        val sut = LocalPaymentApi(braintreeClient)
        val webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId"
        sut.tokenize(
            "local-merchant-account-id", webUrl, "sample-correlation-id",
            localPaymentInternalTokenizeCallback!!
        )

        val bodyCaptor = ArgumentCaptor.forClass(
            String::class.java
        )
        val expectedUrl = "/v1/payment_methods/paypal_accounts"

        Mockito.verify(braintreeClient).sendPOST(
            url = ArgumentMatchers.eq(expectedUrl), data = bodyCaptor.capture(),
            responseCallback = ArgumentMatchers.any(HttpResponseCallback::class.java)
        )
        val requestBody = bodyCaptor.value

        val expectedJSON = JSONObject()
        expectedJSON.put("merchant_account_id", "local-merchant-account-id")

        val paypalAccount = JSONObject()
            .put("intent", "sale")
            .put("response", JSONObject().put("webURL", webUrl))
            .put("options", JSONObject().put("validate", false))
            .put("response_type", "web")
            .put("correlation_id", "sample-correlation-id")
        expectedJSON.put("paypal_account", paypalAccount)

        val metaData = JSONObject()
            .put("source", "client")
            .put("integration", "custom")
            .put("sessionId", "sample-session-id")
        expectedJSON.put("_meta", metaData)

        JSONAssert.assertEquals(expectedJSON, JSONObject(requestBody), true)
    }

    @Test
    fun tokenize_onPOSTError_forwardsErrorToCallback() {
        val error = Exception("error")
        val braintreeClient = MockBraintreeClientBuilder()
            .sessionId("sample-session-id")
            .integration(IntegrationType.CUSTOM)
            .sendPOSTErrorResponse(error)
            .build()

        val sut = LocalPaymentApi(braintreeClient)
        val webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId"
        sut.tokenize(
            "local-merchant-account-id", webUrl, "sample-correlation-id",
            localPaymentInternalTokenizeCallback!!
        )

        Mockito.verify(localPaymentInternalTokenizeCallback)
            ?.onResult(ArgumentMatchers.isNull(), ArgumentMatchers.same(error))
    }

    @Test
    fun tokenize_onJSONError_forwardsErrorToCallback() {
        val braintreeClient = MockBraintreeClientBuilder()
            .sessionId("sample-session-id")
            .integration(IntegrationType.CUSTOM)
            .sendPOSTSuccessfulResponse("not-json")
            .build()

        val sut = LocalPaymentApi(braintreeClient)
        val webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId"
        sut.tokenize(
            "local-merchant-account-id", webUrl, "sample-correlation-id",
            localPaymentInternalTokenizeCallback!!
        )

        val captor = ArgumentCaptor.forClass(
            Exception::class.java
        )
        Mockito.verify(localPaymentInternalTokenizeCallback)
            ?.onResult(ArgumentMatchers.isNull(), captor.capture())

        assertTrue(captor.value is JSONException)
    }

    @Test
    fun tokenize_onPOSTSuccess_returnsResultToCallback() {
        val braintreeClient = MockBraintreeClientBuilder()
            .sessionId("sample-session-id")
            .integration(IntegrationType.CUSTOM)
            .sendPOSTSuccessfulResponse(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE)
            .build()

        val sut = LocalPaymentApi(braintreeClient)
        val webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId"
        sut.tokenize(
            "local-merchant-account-id", webUrl, "sample-correlation-id",
            localPaymentInternalTokenizeCallback!!
        )

        val captor = ArgumentCaptor.forClass(
            LocalPaymentNonce::class.java
        )
        Mockito.verify(localPaymentInternalTokenizeCallback)?.onResult(
            captor.capture(),
            ArgumentMatchers.isNull<Any>() as Exception
        )

        val result = captor.value
        assertNotNull(result)
        assertEquals("e11c9c39-d6a4-0305-791d-bfe680ef2d5d", result.string)
        assertEquals("084afbf1db15445587d30bc120a23b09", result.clientMetadataId)
        assertEquals("jon@getbraintree.com", result.email)
        assertEquals("Jon", result.givenName)
        assertEquals("Doe", result.surname)
        assertEquals("9KQSUZTL7YZQ4", result.payerId)

        val shippingAddress = result.shippingAddress
        assertEquals("Jon Doe", shippingAddress.recipientName)
        assertEquals("836486 of 22321 Park Lake", shippingAddress.streetAddress)
    }

    private val idealLocalPaymentRequest: LocalPaymentRequest
        get() {
            val address = PostalAddress()
            address.streetAddress = "836486 of 22321 Park Lake"
            address.extendedAddress = "Apt 2"
            address.countryCodeAlpha2 = "NL"
            address.locality = "Den Haag"
            address.region = "CA"
            address.postalCode = "2585 GJ"

            val request = LocalPaymentRequest(true)
            request.paymentType = "ideal"
            request.amount = "1.10"
            request.address = address
            request.phone = "639847934"
            request.email = "jon@getbraintree.com"
            request.givenName = "Jon"
            request.surname = "Doe"
            request.isShippingAddressRequired = false
            request.merchantAccountId = "local-merchant-account-id"
            request.currencyCode = "EUR"
            request.paymentTypeCountryCode = "NL"
            request.displayName = "My Brand!"

            return request
        }
}
