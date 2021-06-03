package com.braintreepayments.api

import android.os.Parcel
import junit.framework.TestCase.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*

@RunWith(RobolectricTestRunner::class)
class PayPalCheckoutRequestUnitTest {

    @Test
    fun newPayPalCheckoutRequest_setsDefaultValues() {
        val request = PayPalCheckoutRequest("1.00")

        assertNotNull(request.amount)
        assertNull(request.currencyCode)
        assertNull(request.localeCode)
        assertFalse(request.isShippingAddressRequired)
        assertNull(request.shippingAddressOverride)
        assertNull(request.displayName)
        assertEquals(PayPalPaymentIntent.AUTHORIZE, request.intent)
        assertNull(request.landingPageType)
        assertNull(request.billingAgreementDescription)
        assertFalse(request.shouldOfferPayLater)
    }

    @Test
    fun setsValuesCorrectly() {
        val postalAddress = PostalAddress()
        val request = PayPalCheckoutRequest("1.00")
        request.currencyCode = "USD"
        request.shouldOfferPayLater = true
        request.intent = PayPalPaymentIntent.SALE
        request.localeCode = "US"
        request.shouldRequestBillingAgreement = true
        request.billingAgreementDescription = "Billing Agreement Description"
        request.isShippingAddressRequired = true
        request.shippingAddressOverride = postalAddress
        request.userAction = PayPalCheckoutRequest.USER_ACTION_COMMIT
        request.displayName = "Display Name"
        request.landingPageType = PayPalRequest.LANDING_PAGE_TYPE_LOGIN

        assertEquals("1.00", request.amount)
        assertEquals("USD", request.currencyCode)
        assertEquals("US", request.localeCode)
        assertTrue(request.shouldRequestBillingAgreement)
        assertEquals("Billing Agreement Description", request.billingAgreementDescription)
        assertTrue(request.isShippingAddressRequired)
        assertEquals(postalAddress, request.shippingAddressOverride)
        assertEquals(PayPalPaymentIntent.SALE, request.intent)
        assertEquals(PayPalCheckoutRequest.USER_ACTION_COMMIT, request.userAction)
        assertEquals("Display Name", request.displayName)
        assertEquals(PayPalRequest.LANDING_PAGE_TYPE_LOGIN, request.landingPageType)
        assertTrue(request.shouldOfferPayLater)
    }

    @Test
    fun parcelsCorrectly() {
        val request = PayPalCheckoutRequest("12.34")
        request.currencyCode = "USD"
        request.localeCode = "en-US"
        request.billingAgreementDescription = "Billing Agreement Description"
        request.isShippingAddressRequired = true
        request.isShippingAddressEditable = true

        val postalAddress = PostalAddress()
        postalAddress.recipientName = "Postal Address"
        request.shippingAddressOverride = postalAddress
        request.intent = PayPalPaymentIntent.SALE
        request.landingPageType = PayPalRequest.LANDING_PAGE_TYPE_LOGIN
        request.userAction = PayPalCheckoutRequest.USER_ACTION_COMMIT
        request.displayName = "Display Name"
        request.merchantAccountId = "merchant_account_id"

        val lineItems = ArrayList<PayPalLineItem>()
        lineItems.add(PayPalLineItem(PayPalLineItem.KIND_DEBIT, "An Item", "1", "1"))
        request.setLineItems(lineItems)

        val parcel = Parcel.obtain()
        request.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val result = PayPalCheckoutRequest.CREATOR.createFromParcel(parcel)

        assertEquals("12.34", result.amount)
        assertEquals("USD", result.currencyCode)
        assertEquals("en-US", result.localeCode)
        assertEquals("Billing Agreement Description",
                result.billingAgreementDescription)
        assertTrue(result.isShippingAddressRequired)
        assertTrue(result.isShippingAddressEditable)
        assertEquals("Postal Address", result.shippingAddressOverride?.recipientName)
        assertEquals(PayPalPaymentIntent.SALE, result.intent)
        assertEquals(PayPalRequest.LANDING_PAGE_TYPE_LOGIN, result.landingPageType)
        assertEquals(PayPalCheckoutRequest.USER_ACTION_COMMIT, result.userAction)
        assertEquals("Display Name", result.displayName)
        assertEquals("merchant_account_id", result.merchantAccountId)
        assertEquals(1, result.lineItems.size)
        assertEquals("An Item", result.lineItems[0].name)
    }
}