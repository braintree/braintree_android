package com.braintreepayments.api.googlepay

import android.os.Parcel
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import kotlinx.parcelize.parcelableCreator
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class GooglePayRequestTest {

    @Suppress("LongMethod")
    @Test
    fun parcels_withAllFieldsPopulated() {
        val displayItems = mutableListOf(
            GooglePayDisplayItem(
                label = "Subtotal",
                type = GooglePayDisplayItemType.SUBTOTAL,
                price = "9.99",
                status = GooglePayDisplayItemStatus.FINAL
            ),
            GooglePayDisplayItem(
                label = "Tax",
                type = GooglePayDisplayItemType.TAX,
                price = "1.01",
                status = GooglePayDisplayItemStatus.PENDING
            )
        )

        val request = GooglePayRequest(
            currencyCode = "USD",
            totalPrice = "11.00",
            totalPriceStatus = GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL,
            isEmailRequired = true,
            isPhoneNumberRequired = true,
            isBillingAddressRequired = true,
            billingAddressFormat = GooglePayBillingAddressFormat.FULL,
            isShippingAddressRequired = true,
            shippingAddressParameters = GooglePayShippingAddressParameters(
                allowedCountryCodes = listOf("US", "CA"),
                isPhoneNumberRequired = true
            ),
            allowPrepaidCards = true,
            isPayPalEnabled = false,
            googleMerchantName = "Test Merchant",
            countryCode = "US",
            totalPriceLabel = "Total",
            allowCreditCards = false,
            displayItems = displayItems,
            checkoutOption = GooglePayCheckoutOption.COMPLETE_IMMEDIATE_PURCHASE
        )
        request.setEnvironment("production")

        val parcel = Parcel.obtain()
        request.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val parceled = parcelableCreator<GooglePayRequest>().createFromParcel(parcel)

        assertEquals("USD", parceled.currencyCode)
        assertEquals("11.00", parceled.totalPrice)
        assertEquals(GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL, parceled.totalPriceStatus)
        assertTrue(parceled.isEmailRequired)
        assertTrue(parceled.isPhoneNumberRequired)
        assertTrue(parceled.isBillingAddressRequired)
        assertEquals(GooglePayBillingAddressFormat.FULL, parceled.billingAddressFormat)
        assertTrue(parceled.isShippingAddressRequired)
        assertEquals(listOf("US", "CA"), parceled.shippingAddressParameters?.allowedCountryCodes)
        assertTrue(parceled.shippingAddressParameters?.isPhoneNumberRequired == true)
        assertTrue(parceled.allowPrepaidCards)
        assertFalse(parceled.isPayPalEnabled)
        assertEquals("Test Merchant", parceled.googleMerchantName)
        assertEquals("US", parceled.countryCode)
        assertEquals("Total", parceled.totalPriceLabel)
        assertFalse(parceled.allowCreditCards)
        assertEquals(2, parceled.displayItems.size)
        assertEquals("Subtotal", parceled.displayItems[0].label)
        assertEquals("Tax", parceled.displayItems[1].label)
        assertEquals(GooglePayCheckoutOption.COMPLETE_IMMEDIATE_PURCHASE, parceled.checkoutOption)
        assertEquals("PRODUCTION", parceled.getEnvironment())

        parcel.recycle()
    }

    @Test
    fun parcels_withRequiredFieldsOnly() {
        val request = GooglePayRequest(
            currencyCode = "EUR",
            totalPrice = "5.50",
            totalPriceStatus = GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_ESTIMATED
        )

        val parcel = Parcel.obtain()
        request.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val parceled = parcelableCreator<GooglePayRequest>().createFromParcel(parcel)

        assertEquals("EUR", parceled.currencyCode)
        assertEquals("5.50", parceled.totalPrice)
        assertEquals(GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_ESTIMATED, parceled.totalPriceStatus)
        assertFalse(parceled.isEmailRequired)
        assertFalse(parceled.isPhoneNumberRequired)
        assertFalse(parceled.isBillingAddressRequired)
        assertEquals(GooglePayBillingAddressFormat.MIN, parceled.billingAddressFormat)
        assertFalse(parceled.isShippingAddressRequired)
        assertNull(parceled.shippingAddressParameters)
        assertFalse(parceled.allowPrepaidCards)
        assertTrue(parceled.isPayPalEnabled)
        assertNull(parceled.googleMerchantName)
        assertNull(parceled.countryCode)
        assertNull(parceled.totalPriceLabel)
        assertTrue(parceled.allowCreditCards)
        assertTrue(parceled.displayItems.isEmpty())
        assertEquals(GooglePayCheckoutOption.DEFAULT, parceled.checkoutOption)
        assertNull(parceled.getEnvironment())

        parcel.recycle()
    }

    @Test
    fun parcels_preservesCustomPaymentMethodsAndTokenizationSpecs() {
        val request = GooglePayRequest("USD", "1.00", GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL)

        val cardParams = JSONObject()
            .put("allowedAuthMethods", JSONArray().put("PAN_ONLY"))
            .put("allowedCardNetworks", JSONArray().put("VISA"))
        request.setAllowedPaymentMethod("CARD", cardParams)

        val tokenSpec = JSONObject()
            .put("type", "PAYMENT_GATEWAY")
            .put("parameters", JSONObject().put("gateway", "braintree"))
        request.setTokenizationSpecificationForType("CARD", tokenSpec)

        request.setAllowedAuthMethods("CARD", JSONArray().put("PAN_ONLY").put("CRYPTOGRAM_3DS"))
        request.setAllowedCardNetworks("CARD", JSONArray().put("VISA").put("MASTERCARD"))

        val parcel = Parcel.obtain()
        request.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val parceled = parcelableCreator<GooglePayRequest>().createFromParcel(parcel)
        val json = JSONObject(parceled.toJson())
        val allowedPaymentMethods = json.getJSONArray("allowedPaymentMethods")

        assertEquals(1, allowedPaymentMethods.length())
        assertEquals("CARD", allowedPaymentMethods.getJSONObject(0).getString("type"))

        parcel.recycle()
    }

    @Test
    fun toJson_producesCorrectTransactionInfo() {
        val request = GooglePayRequest(
            currencyCode = "USD",
            totalPrice = "25.00",
            totalPriceStatus = GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL,
            countryCode = "US",
            totalPriceLabel = "Order Total",
            checkoutOption = GooglePayCheckoutOption.COMPLETE_IMMEDIATE_PURCHASE
        )
        request.setEnvironment("sandbox")

        val json = JSONObject(request.toJson())

        assertEquals(2, json.getInt("apiVersion"))
        assertEquals(0, json.getInt("apiVersionMinor"))
        assertEquals("TEST", json.getString("environment"))

        val transactionInfo = json.getJSONObject("transactionInfo")
        assertEquals("FINAL", transactionInfo.getString("totalPriceStatus"))
        assertEquals("25.00", transactionInfo.getString("totalPrice"))
        assertEquals("USD", transactionInfo.getString("currencyCode"))
        assertEquals("US", transactionInfo.getString("countryCode"))
        assertEquals("Order Total", transactionInfo.getString("totalPriceLabel"))
        assertEquals("COMPLETE_IMMEDIATE_PURCHASE", transactionInfo.getString("checkoutOption"))
    }

    @Test
    fun toJson_includesDisplayItems() {
        val displayItems = mutableListOf(
            GooglePayDisplayItem(
                label = "Subtotal",
                type = GooglePayDisplayItemType.SUBTOTAL,
                price = "10.00"
            ),
            GooglePayDisplayItem(
                label = "Shipping",
                type = GooglePayDisplayItemType.SHIPPING_OPTION,
                price = "5.00",
                status = GooglePayDisplayItemStatus.PENDING
            )
        )

        val request = GooglePayRequest(
            currencyCode = "USD",
            totalPrice = "15.00",
            totalPriceStatus = GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL,
            displayItems = displayItems
        )

        val json = JSONObject(request.toJson())
        val transactionInfo = json.getJSONObject("transactionInfo")
        val items = transactionInfo.getJSONArray("displayItems")

        assertEquals(2, items.length())
        assertEquals("Subtotal", items.getJSONObject(0).getString("label"))
        assertEquals("SUBTOTAL", items.getJSONObject(0).getString("type"))
        assertEquals("10.00", items.getJSONObject(0).getString("price"))
        assertEquals("FINAL", items.getJSONObject(0).getString("status"))
        assertEquals("Shipping", items.getJSONObject(1).getString("label"))
        assertEquals("PENDING", items.getJSONObject(1).getString("status"))
    }

    @Test
    fun toJson_includesShippingAddressParameters() {
        val request = GooglePayRequest(
            currencyCode = "USD",
            totalPrice = "1.00",
            totalPriceStatus = GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL,
            isShippingAddressRequired = true,
            shippingAddressParameters = GooglePayShippingAddressParameters(
                allowedCountryCodes = listOf("US", "GB"),
                isPhoneNumberRequired = true
            )
        )

        val json = JSONObject(request.toJson())

        assertTrue(json.getBoolean("shippingAddressRequired"))
        val shippingParams = json.getJSONObject("shippingAddressParameters")
        val countryCodes = shippingParams.getJSONArray("allowedCountryCodes")
        assertEquals(2, countryCodes.length())
        assertEquals("US", countryCodes.getString(0))
        assertEquals("GB", countryCodes.getString(1))
        assertTrue(shippingParams.getBoolean("phoneNumberRequired"))
    }

    @Test
    fun toJson_includesMerchantInfo() {
        val request = GooglePayRequest(
            currencyCode = "USD",
            totalPrice = "1.00",
            totalPriceStatus = GooglePayTotalPriceStatus.TOTAL_PRICE_STATUS_FINAL,
            googleMerchantName = "My Merchant"
        )

        val json = JSONObject(request.toJson())
        val merchantInfo = json.getJSONObject("merchantInfo")

        assertEquals("My Merchant", merchantInfo.getString("merchantName"))
        assertTrue(merchantInfo.has("softwareInfo"))
    }
}
