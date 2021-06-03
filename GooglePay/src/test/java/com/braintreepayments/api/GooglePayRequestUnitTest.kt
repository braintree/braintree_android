package com.braintreepayments.api

import android.os.Parcel
import com.google.android.gms.wallet.ShippingAddressRequirements
import com.google.android.gms.wallet.TransactionInfo
import com.google.android.gms.wallet.WalletConstants
import junit.framework.TestCase.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.skyscreamer.jsonassert.JSONAssert

@RunWith(RobolectricTestRunner::class)
class GooglePayRequestUnitTest {

    @Test
    fun returnsAllValues() {
        val shippingAddressRequirements = ShippingAddressRequirements.newBuilder().build()
        val transactionInfo = TransactionInfo.newBuilder()
                .setCurrencyCode("USD")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_NOT_CURRENTLY_KNOWN)
                .build()

        val request = GooglePayRequest()
        request.allowPrepaidCards = true
        request.billingAddressFormat = WalletConstants.BILLING_ADDRESS_FORMAT_FULL
        request.isBillingAddressRequired = true
        request.isEmailRequired = true
        request.isPhoneNumberRequired = true
        request.isShippingAddressRequired = true
        request.shippingAddressRequirements = shippingAddressRequirements
        request.transactionInfo = transactionInfo
        request.environment = "production"
        request.googleMerchantId = "google-merchant-id"
        request.googleMerchantName = "google-merchant-name"

        assertTrue(request.allowPrepaidCards)
        assertEquals(WalletConstants.BILLING_ADDRESS_FORMAT_FULL, request.billingAddressFormat)
        assertTrue(request.isBillingAddressRequired)
        assertTrue(request.isEmailRequired)
        assertTrue(request.isPhoneNumberRequired)
        assertTrue(request.isShippingAddressRequired)
        assertEquals(shippingAddressRequirements, request.shippingAddressRequirements)
        assertEquals(transactionInfo, request.transactionInfo)
        assertEquals("PRODUCTION", request.environment)
        assertEquals("google-merchant-id", request.googleMerchantId)
        assertEquals("google-merchant-name", request.googleMerchantName)
    }

    @Test
    fun constructor_setsDefaultValues() {
        val request = GooglePayRequest()

        assertFalse(request.allowPrepaidCards)
        assertEquals(WalletConstants.BILLING_ADDRESS_FORMAT_MIN, request.billingAddressFormat)
        assertFalse(request.isBillingAddressRequired)
        assertFalse(request.isEmailRequired)
        assertFalse(request.isPhoneNumberRequired)
        assertFalse(request.isShippingAddressRequired)
        assertNull(request.shippingAddressRequirements)
        assertNull(request.transactionInfo)
        assertNull(request.environment)
        assertNull(request.environment)
        assertNull(request.googleMerchantId)
        assertNull(request.googleMerchantName)
    }

    @Test
    fun parcelsCorrectly() {
        val request = GooglePayRequest()
        val info = TransactionInfo.newBuilder()
                .setCurrencyCode("USD")
                .setTotalPrice("10")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .build()
        request.transactionInfo = info
        request.isEmailRequired = true
        request.isPhoneNumberRequired = true
        request.isShippingAddressRequired = true
        request.isBillingAddressRequired = true
        request.billingAddressFormat = WalletConstants.BILLING_ADDRESS_FORMAT_FULL

        val requirements = ShippingAddressRequirements.newBuilder()
                .addAllowedCountryCode("US")
                .build()
        request.shippingAddressRequirements = requirements
        request.allowPrepaidCards = true
        request.environment = "production"

        val parcel = Parcel.obtain()
        request.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val parceled = GooglePayRequest.CREATOR.createFromParcel(parcel)

        assertEquals("USD", parceled.transactionInfo.currencyCode)
        assertEquals("10", parceled.transactionInfo.totalPrice)
        assertEquals(WalletConstants.TOTAL_PRICE_STATUS_FINAL, parceled.transactionInfo.totalPriceStatus)
        assertTrue(parceled.isEmailRequired)
        assertTrue(parceled.isPhoneNumberRequired)
        assertTrue(parceled.isShippingAddressRequired)
        assertTrue(parceled.isBillingAddressRequired)
        assertEquals(WalletConstants.BILLING_ADDRESS_FORMAT_FULL, parceled.billingAddressFormat)
        assertTrue(parceled.shippingAddressRequirements!!.allowedCountryCodes!!.contains("US"))
        assertTrue(parceled.allowPrepaidCards)
        assertEquals("PRODUCTION", parceled.environment)
    }

    @Test
    fun parcelsCorrectly_allFieldsPopulated_null() {
        val request = GooglePayRequest()
        val info = TransactionInfo.newBuilder()
                .setCurrencyCode("USD")
                .setTotalPrice("10")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .build()

        request.transactionInfo = info
        request.billingAddressFormat = WalletConstants.BILLING_ADDRESS_FORMAT_FULL
        val requirements = ShippingAddressRequirements.newBuilder()
                .addAllowedCountryCode("US")
                .build()
        request.shippingAddressRequirements = requirements

        val parcel = Parcel.obtain()
        request.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val parceled = GooglePayRequest.CREATOR.createFromParcel(parcel)

        assertEquals("USD", parceled.transactionInfo.currencyCode)
        assertEquals("10", parceled.transactionInfo.totalPrice)
        assertEquals(WalletConstants.TOTAL_PRICE_STATUS_FINAL, parceled.transactionInfo.totalPriceStatus)
        assertFalse(parceled.isEmailRequired)
        assertFalse(parceled.isPhoneNumberRequired)
        assertFalse(parceled.isShippingAddressRequired)
        assertFalse(parceled.isBillingAddressRequired)
        assertEquals(WalletConstants.BILLING_ADDRESS_FORMAT_FULL, parceled.billingAddressFormat)
        assertTrue(parceled.shippingAddressRequirements!!.allowedCountryCodes!!.contains("US"))
        assertFalse(parceled.allowPrepaidCards)
        assertNull(parceled.environment)
        assertNull(parceled.googleMerchantId)
        assertNull(parceled.googleMerchantName)
    }

    @Test
    @Throws(JSONException::class)
    fun generatesToJsonRequest() {
        val request = GooglePayRequest()
        val expected = Fixtures.PAYMENT_METHODS_GOOGLE_PAY_REQUEST
        val shippingAllowedCountryCodes = listOf("US", "CA", "MX", "GB")

        val shippingAddressRequirements = ShippingAddressRequirements.newBuilder()
                .addAllowedCountryCodes(shippingAllowedCountryCodes)
                .build()

        val info = TransactionInfo.newBuilder()
                .setCurrencyCode("USD")
                .setTotalPrice("12.24")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .build()

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
                .put("purchase_context", "{\"purchase_context\":{\"purchase_units\":[{\"payee\":{\"client_id\":\"FAKE_PAYPAL_CLIENT_ID\"},\"recurring_payment\":false}]}}")

        request.transactionInfo = info
        request.countryCode = "US"
        request.isPhoneNumberRequired = true
        request.isEmailRequired = true
        request.isShippingAddressRequired = true
        request.shippingAddressRequirements = shippingAddressRequirements
        request.isBillingAddressRequired = true
        request.allowPrepaidCards = true
        request.setAllowedPaymentMethod("CARD", cardAllowedPaymentMethodParams)
        request.setTokenizationSpecificationForType("CARD", tokenizationSpecificationParams)
        request.setAllowedPaymentMethod("PAYPAL", paypalAllowedPaymentMethodParams)
        request.setTokenizationSpecificationForType("PAYPAL", tokenizationSpecificationParams)
        request.environment = "production"
        request.googleMerchantId = "GOOGLE_MERCHANT_ID"
        request.googleMerchantName = "GOOGLE_MERCHANT_NAME"

        val actual = request.toJson()

        JSONAssert.assertEquals(expected, actual, false)
    }

    @Test
    @Throws(JSONException::class)
    fun allowsNullyOptionalParameters() {
        val request = GooglePayRequest()
        val expected = "{\"apiVersion\":2,\"apiVersionMinor\":0,\"allowedPaymentMethods\":[],\"shippingAddressRequired\":true,\"merchantInfo\":{},\"transactionInfo\":{\"totalPriceStatus\":\"FINAL\",\"totalPrice\":\"12.24\",\"currencyCode\":\"USD\"},\"shippingAddressParameters\":{}}"
        val nullyShippingAddressRequirements = ShippingAddressRequirements.newBuilder().build()

        val info = TransactionInfo.newBuilder()
                .setCurrencyCode("USD")
                .setTotalPrice("12.24")
                .setTotalPriceStatus(WalletConstants.TOTAL_PRICE_STATUS_FINAL)
                .build()

        request.transactionInfo = info
        request.isShippingAddressRequired = true
        request.shippingAddressRequirements = nullyShippingAddressRequirements

        val actual = request.toJson()

        JSONAssert.assertEquals(expected, actual, false)
    }
}