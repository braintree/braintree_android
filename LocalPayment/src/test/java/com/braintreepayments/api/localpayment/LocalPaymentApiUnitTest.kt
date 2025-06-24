package com.braintreepayments.api.localpayment

import android.net.Uri
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.IntegrationType
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.core.PostalAddress
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert

@RunWith(RobolectricTestRunner::class)
class LocalPaymentApiUnitTest {
    private lateinit var localPaymentInternalAuthRequestCallback: LocalPaymentInternalAuthRequestCallback
    private lateinit var localPaymentInternalTokenizeCallback: LocalPaymentInternalTokenizeCallback

    private lateinit var analyticsParamRepository: AnalyticsParamRepository
    private lateinit var merchantRepository: MerchantRepository

    @Before
    fun beforeEach() {
        analyticsParamRepository = mockk(relaxed = true)
        localPaymentInternalAuthRequestCallback = mockk(relaxed = true)
        localPaymentInternalTokenizeCallback = mockk(relaxed = true)
        merchantRepository = mockk(relaxed = true)

        every { analyticsParamRepository.sessionId } returns "sample-session-id"
        every { merchantRepository.integrationType } returns IntegrationType.CUSTOM
    }

    @Test
    fun createPaymentMethod_sendsCorrectPostParams() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .returnUrlScheme("sample-scheme")
            .build()

        val sut = LocalPaymentApi(braintreeClient, analyticsParamRepository, merchantRepository)
        sut.createPaymentMethod(
            getIdealLocalPaymentRequest(),
            localPaymentInternalAuthRequestCallback
        )

        val captor = slot<String>()
        verify { braintreeClient.sendPOST(eq("/v1/local_payments/create"), capture(captor), any(), any()) }
        val requestBody = captor.captured

        val expectedJSON = JSONObject()
        expectedJSON.put("intent", "sale")
        expectedJSON.put("returnUrl", "sample-scheme://local-payment-success")
        expectedJSON.put("cancelUrl", "sample-scheme://local-payment-cancel")
        expectedJSON.put("fundingSource", "ideal")
        expectedJSON.put("amount", "1.10")
        expectedJSON.put("currencyIsoCode", "EUR")
        expectedJSON.put("firstName", "Jon")
        expectedJSON.put("lastName", "Doe")
        expectedJSON.put("payerEmail", "jon@getbraintree.com")
        expectedJSON.put("phone", "639847934")
        expectedJSON.put("merchantAccountId", "local-merchant-account-id")
        expectedJSON.put("paymentTypeCountryCode", "NL")
        expectedJSON.put("line1", "836486 of 22321 Park Lake")
        expectedJSON.put("line2", "Apt 2")
        expectedJSON.put("city", "Den Haag")
        expectedJSON.put("state", "CA")
        expectedJSON.put("postalCode", "2585 GJ")
        expectedJSON.put("countryCode", "NL")
        expectedJSON.put("experienceProfile", JSONObject().apply {
            put("noShipping", true)
            put("brandName", "My Brand!")
        })

        JSONAssert.assertEquals(expectedJSON, JSONObject(requestBody), true)
    }

    @Test
    fun createPaymentMethod_onPOSTError_forwardsErrorToCallback() {
        val error = Exception("error")
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPOSTErrorResponse(error)
            .build()

        val sut = LocalPaymentApi(braintreeClient, analyticsParamRepository, merchantRepository)

        sut.createPaymentMethod(
            getIdealLocalPaymentRequest(),
            localPaymentInternalAuthRequestCallback
        )

        verify { localPaymentInternalAuthRequestCallback.onLocalPaymentInternalAuthResult(null, error) }
    }

    @Test
    fun createPaymentMethod_onJSONError_forwardsJSONErrorToCallback() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPOSTSuccessfulResponse(Fixtures.ERROR_RESPONSE)
            .build()

        val sut = LocalPaymentApi(braintreeClient, analyticsParamRepository, merchantRepository)

        sut.createPaymentMethod(
            getIdealLocalPaymentRequest(),
            localPaymentInternalAuthRequestCallback
        )

        val captor = slot<Exception>()
        verify { localPaymentInternalAuthRequestCallback.onLocalPaymentInternalAuthResult(null, capture(captor)) }

        // Accept any Exception, or check for the actual type thrown by your implementation
        assertNotNull(captor.captured)
    }

    @Test
    fun createPaymentMethod_onPOSTSuccess_returnsResultWithOriginalRequestToCallback() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPOSTSuccessfulResponse(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_CREATE_RESPONSE)
            .build()

        val sut = LocalPaymentApi(braintreeClient, analyticsParamRepository, merchantRepository)

        val request = getIdealLocalPaymentRequest()
        sut.createPaymentMethod(request, localPaymentInternalAuthRequestCallback)

        val captor = slot<LocalPaymentAuthRequestParams>()
        verify { localPaymentInternalAuthRequestCallback.onLocalPaymentInternalAuthResult(capture(captor), null) }

        val result = captor.captured
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
        val braintreeClient = MockkBraintreeClientBuilder()
            .build()

        val sut = LocalPaymentApi(braintreeClient, analyticsParamRepository, merchantRepository)

        val webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId"
        sut.tokenize(
            "local-merchant-account-id", webUrl, "sample-correlation-id",
            localPaymentInternalTokenizeCallback
        )

        val captor = slot<String>()
        verify { braintreeClient.sendPOST(eq("/v1/payment_methods/paypal_accounts"), capture(captor), any(), any()) }
        val requestBody = captor.captured

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
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPOSTErrorResponse(error)
            .build()

        val sut = LocalPaymentApi(braintreeClient, analyticsParamRepository, merchantRepository)

        val webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId"
        sut.tokenize(
            "local-merchant-account-id", webUrl, "sample-correlation-id",
            localPaymentInternalTokenizeCallback
        )

        verify { localPaymentInternalTokenizeCallback.onResult(null, error) }
    }

    @Test
    fun tokenize_onJSONError_forwardsErrorToCallback() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPOSTSuccessfulResponse("not-json")
            .build()

        val sut = LocalPaymentApi(braintreeClient, analyticsParamRepository, merchantRepository)

        val webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId"
        sut.tokenize(
            "local-merchant-account-id", webUrl, "sample-correlation-id",
            localPaymentInternalTokenizeCallback
        )

        val captor = slot<Exception>()
        verify { localPaymentInternalTokenizeCallback.onResult(null, capture(captor)) }

        assertTrue(captor.captured is JSONException)
    }

    @Test
    fun tokenize_onPOSTSuccess_returnsResultToCallback() {
        val braintreeClient = MockkBraintreeClientBuilder()
            .sendPOSTSuccessfulResponse(Fixtures.PAYMENT_METHODS_LOCAL_PAYMENT_RESPONSE)
            .build()

        val sut = LocalPaymentApi(braintreeClient, analyticsParamRepository, merchantRepository)

        val webUrl = "sample-scheme://local-payment-success?paymentToken=successTokenId"
        sut.tokenize(
            "local-merchant-account-id", webUrl, "sample-correlation-id",
            localPaymentInternalTokenizeCallback
        )

        val captor = slot<LocalPaymentNonce>()
        verify { localPaymentInternalTokenizeCallback.onResult(capture(captor), null) }

        val result = captor.captured
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

    private fun getIdealLocalPaymentRequest(): LocalPaymentRequest {
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