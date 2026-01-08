package com.braintreepayments.api.googlepay

import android.os.Parcel
import com.braintreepayments.api.testutils.Fixtures
import kotlinx.parcelize.parcelableCreator
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class GooglePayRequestUnitTest {

    @Test
    fun `returns all assigned values`() {
        val shippingAddressRequirements = GooglePayShippingAddressParameters()
        val request = GooglePayRequest("USD", "1.00", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL)

        request.totalPriceLabel = "test"
        request.allowPrepaidCards = true
        request.billingAddressFormat = GooglePayBillingAddressFormat.FULL
        request.isBillingAddressRequired = true
        request.isEmailRequired = true
        request.isPhoneNumberRequired = true
        request.isShippingAddressRequired = true
        request.shippingAddressParameters = shippingAddressRequirements
        request.setEnvironment("production")
        request.googleMerchantName = "google-merchant-name"

        assertTrue(request.allowPrepaidCards)
        assertEquals(GooglePayBillingAddressFormat.FULL, request.billingAddressFormat)
        assertTrue(request.isBillingAddressRequired)
        assertTrue(request.isEmailRequired)
        assertTrue(request.isPhoneNumberRequired)
        assertTrue(request.isShippingAddressRequired)
        assertEquals(shippingAddressRequirements, request.shippingAddressParameters)
        assertEquals("USD", request.currencyCode)
        assertEquals("1.00", request.totalPrice)
        assertEquals(GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL, request.totalPriceStatus)
        assertEquals("PRODUCTION", request.getEnvironment())
        assertEquals("google-merchant-name", request.googleMerchantName)
        assertEquals("test", request.totalPriceLabel)
    }

    @Test
    fun `sets default values via constructor`() {
        val request = GooglePayRequest("USD", "1.00", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL)

        assertFalse(request.allowPrepaidCards)
        assertEquals(GooglePayBillingAddressFormat.MIN, request.billingAddressFormat)
        assertFalse(request.isBillingAddressRequired)
        assertFalse(request.isEmailRequired)
        assertFalse(request.isPhoneNumberRequired)
        assertFalse(request.isShippingAddressRequired)
        assertTrue(request.allowCreditCards)
        assertNull(request.getEnvironment())
        assertNull(request.googleMerchantName)
    }

    @Test
    fun `parcels GooglePay request with all fields populated`() {
        val request = GooglePayRequest("USD", "1.00", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL)

        request.totalPriceLabel = "test"
        request.isEmailRequired = true
        request.isPhoneNumberRequired = true
        request.isShippingAddressRequired = true
        request.isBillingAddressRequired = true
        request.billingAddressFormat = GooglePayBillingAddressFormat.FULL

        val requirements = GooglePayShippingAddressParameters(listOf("US"), true)

        request.shippingAddressParameters = requirements
        request.allowPrepaidCards = true
        request.setEnvironment("production")

        val parcel = Parcel.obtain()
        request.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val parceled = parcelableCreator<GooglePayRequest>().createFromParcel(parcel)

        assertEquals("USD", parceled.currencyCode)
        assertEquals("1.00", parceled.totalPrice)
        assertEquals(GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL, parceled.totalPriceStatus)
        assertEquals("test", parceled.totalPriceLabel)
        assertTrue(parceled.isEmailRequired)
        assertTrue(parceled.isPhoneNumberRequired)
        assertTrue(parceled.isShippingAddressRequired)
        assertTrue(parceled.isBillingAddressRequired)
        assertEquals(GooglePayBillingAddressFormat.FULL, parceled.billingAddressFormat)
        assertTrue { parceled.shippingAddressParameters?.allowedCountryCodes?.contains("US") == true }
        assertTrue(parceled.allowPrepaidCards)
        assertEquals("PRODUCTION", parceled.getEnvironment())
    }

    @Test
    fun `parcels GooglePay request with some fields populated`() {
        val request = GooglePayRequest("USD", "1.00", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL)

        request.billingAddressFormat = GooglePayBillingAddressFormat.FULL

        val requirements = GooglePayShippingAddressParameters(listOf("US"), true)
        request.shippingAddressParameters = requirements

        val parcel = Parcel.obtain()
        request.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val parceled = parcelableCreator<GooglePayRequest>().createFromParcel(parcel)

        assertEquals("USD", parceled.currencyCode)
        assertEquals("1.00", parceled.totalPrice)
        assertEquals(GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL, parceled.totalPriceStatus)
        assertFalse(parceled.isEmailRequired)
        assertFalse(parceled.isPhoneNumberRequired)
        assertFalse(parceled.isShippingAddressRequired)
        assertFalse(parceled.isBillingAddressRequired)
        assertEquals(GooglePayBillingAddressFormat.FULL, parceled.billingAddressFormat)
        assertTrue { parceled.shippingAddressParameters?.allowedCountryCodes?.contains("US") == true }
        assertFalse(parceled.allowPrepaidCards)
        assertNull(parceled.getEnvironment())
        assertNull(parceled.googleMerchantName)
        assertNull(parceled.totalPriceLabel)
    }

    @Test
    @Throws(JSONException::class)
    fun `generates GooglePay request and produces correct JSON output`() {
        val request = GooglePayRequest("USD", "12.24", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL)
        val expected = Fixtures.PAYMENT_METHODS_GOOGLE_PAY_REQUEST
        val shippingAllowedCountryCodes = listOf("US", "CA", "MX", "GB")

        val requirements = GooglePayShippingAddressParameters(shippingAllowedCountryCodes, true)

        val tokenizationSpecificationParams = JSONObject()
            .put("type", "PAYMENT_GATEWAY")
            .put("parameters", JSONObject()
                .put("gateway", "braintree")
                .put("braintree:apiVersion", "v1")
                .put("braintree:sdkVersion", "BETA")
                .put("braintree:merchantId", "BRAINTREE_MERCHANT_ID")
                .put("braintree:authorizationFingerprint", "BRAINTREE_AUTH_FINGERPRINT")
            )

        val cardAllowedAuthMethods = JSONArray()
            .put("PAN_ONLY")
            .put("CRYPTOGRAM_3DS")

        val cardAllowedCardNetworks = JSONArray()
            .put("VISA")
            .put("AMEX")
            .put("JCB")
            .put("DISCOVER")
            .put("MASTERCARD")

        val cardAllowedPaymentMethodParams = JSONObject()
            .put("allowedAuthMethods", cardAllowedAuthMethods)
            .put("allowedCardNetworks", cardAllowedCardNetworks)

        val paypalAllowedPaymentMethodParams = JSONObject()
            .put("purchase_context", "{" +
                    "\"purchase_context\":{\"purchase_units\":" +
                    "[{\"payee\":{\"client_id\":\"FAKE_PAYPAL_CLIENT_ID\"},\"recurring_payment\":false}]}" +
                    "}")
        request.countryCode = "US"
        request.isPhoneNumberRequired = true
        request.isEmailRequired = true
        request.isShippingAddressRequired = true
        request.shippingAddressParameters = requirements
        request.isBillingAddressRequired = true
        request.allowPrepaidCards = true
        request.allowCreditCards = true
        request.setAllowedPaymentMethod("CARD", cardAllowedPaymentMethodParams)
        request.setTokenizationSpecificationForType("CARD", tokenizationSpecificationParams)
        request.setAllowedPaymentMethod("PAYPAL", paypalAllowedPaymentMethodParams)
        request.setTokenizationSpecificationForType("PAYPAL", tokenizationSpecificationParams)
        request.totalPriceLabel = "Test Label"

        request.setEnvironment("production")
        request.googleMerchantName = "GOOGLE_MERCHANT_NAME"

        val actual = request.toJson()

        JSONAssert.assertEquals(expected, actual, false)
    }

    @Test
    @Throws(JSONException::class)
    fun `produces correct JSON output, when Credit Cards are not allowed and Billing Address required`() {
        val request = GooglePayRequest("USD", "12.24", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL)
        val expected = Fixtures.PAYMENT_METHODS_GOOGLE_PAY_REQUEST_NO_CREDIT_CARDS
        val shippingAllowedCountryCodes = listOf("US", "CA", "MX", "GB")

        val requirements = GooglePayShippingAddressParameters(shippingAllowedCountryCodes, true)

        val tokenizationSpecificationParams = JSONObject()
            .put("type", "PAYMENT_GATEWAY")
            .put("parameters", JSONObject()
                .put("gateway", "braintree")
                .put("braintree:apiVersion", "v1")
                .put("braintree:sdkVersion", "BETA")
                .put("braintree:merchantId", "BRAINTREE_MERCHANT_ID")
                .put("braintree:authorizationFingerprint", "BRAINTREE_AUTH_FINGERPRINT")
            )

        val cardAllowedAuthMethods = JSONArray()
            .put("PAN_ONLY")
            .put("CRYPTOGRAM_3DS")

        val cardAllowedCardNetworks = JSONArray()
            .put("VISA")
            .put("AMEX")
            .put("JCB")
            .put("DISCOVER")
            .put("MASTERCARD")

        val cardAllowedPaymentMethodParams = JSONObject()
            .put("allowedAuthMethods", cardAllowedAuthMethods)
            .put("allowedCardNetworks", cardAllowedCardNetworks)

        val paypalAllowedPaymentMethodParams = JSONObject()
            .put("purchase_context", "{" +
                    "\"purchase_context\":{\"purchase_units\":" +
                    "[{\"payee\":{\"client_id\":\"FAKE_PAYPAL_CLIENT_ID\"},\"recurring_payment\":false}]}" +
                    "}")
        request.countryCode = "US"
        request.isPhoneNumberRequired = true
        request.isEmailRequired = true
        request.isShippingAddressRequired = true
        request.shippingAddressParameters = requirements
        request.isBillingAddressRequired = true
        request.allowPrepaidCards = true
        request.allowCreditCards = false
        request.setAllowedPaymentMethod("CARD", cardAllowedPaymentMethodParams)
        request.setTokenizationSpecificationForType("CARD", tokenizationSpecificationParams)
        request.setAllowedPaymentMethod("PAYPAL", paypalAllowedPaymentMethodParams)
        request.setTokenizationSpecificationForType("PAYPAL", tokenizationSpecificationParams)
        request.totalPriceLabel = "Test Label"

        request.setEnvironment("production")
        request.googleMerchantName = "GOOGLE_MERCHANT_NAME"

        val actual = request.toJson()

        JSONAssert.assertEquals(expected, actual, false)
    }

    @Test
    @Throws(JSONException::class)
    fun `generates GooglePay request and produces correct JSON output with nully optional parameters`() {
        val request = GooglePayRequest("USD", "12.24", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL)
        val expected = "{" +
                "\"apiVersion\":2,\"apiVersionMinor\":0,\"allowedPaymentMethods\":[]," +
                "\"shippingAddressRequired\":true," +
                "\"merchantInfo\":{},\"transactionInfo\":" +
                "{\"totalPriceStatus\":\"FINAL\",\"totalPrice\":\"12.24\"," +
                "\"currencyCode\":\"USD\"},\"shippingAddressParameters\":{}}"

        val nullyShippingAddressRequirements = GooglePayShippingAddressParameters()

        request.isShippingAddressRequired = true
        request.shippingAddressParameters = nullyShippingAddressRequirements

        val actual = request.toJson()

        JSONAssert.assertEquals(expected, actual, false)
    }

    @Test
    fun `toJson includes checkoutOption when set`() {
        val request = GooglePayRequest("USD", "1.00", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL)
        request.checkoutOption = GooglePayCheckoutOption.DEFAULT

        val json = JSONObject(request.toJson())
        assertEquals("DEFAULT", json.optString("checkoutOption"))

        request.checkoutOption = GooglePayCheckoutOption.COMPLETE_IMMEDIATE_PURCHASE
        val json2 = JSONObject(request.toJson())
        assertEquals("COMPLETE_IMMEDIATE_PURCHASE", json2.optString("checkoutOption"))
    }
}
