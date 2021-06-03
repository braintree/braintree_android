package com.braintreepayments.api

import android.os.Parcel
import junit.framework.TestCase.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*

@RunWith(RobolectricTestRunner::class)
class PayPalVaultRequestUnitTest {

    @Test
    fun newPayPalVaultRequest_setsDefaultValues() {
        val request = PayPalVaultRequest()

        assertNull(request.localeCode)
        assertFalse(request.isShippingAddressRequired)
        assertNull(request.shippingAddressOverride)
        assertNull(request.displayName)
        assertNull(request.landingPageType)
        assertFalse(request.shouldOfferCredit)
    }

    @Test
    fun setsValuesCorrectly() {
        val postalAddress = PostalAddress()
        val request = PayPalVaultRequest()

        request.localeCode = "US"
        request.billingAgreementDescription = "Billing Agreement Description"
        request.isShippingAddressRequired = true
        request.shippingAddressOverride = postalAddress
        request.displayName = "Display Name"
        request.landingPageType = PayPalRequest.LANDING_PAGE_TYPE_LOGIN
        request.shouldOfferCredit = true
        assertEquals("US", request.localeCode)
        assertEquals("Billing Agreement Description", request.billingAgreementDescription)
        assertTrue(request.isShippingAddressRequired)
        assertEquals(postalAddress, request.shippingAddressOverride)
        assertEquals("Display Name", request.displayName)
        assertEquals(PayPalRequest.LANDING_PAGE_TYPE_LOGIN, request.landingPageType)
        assertTrue(request.shouldOfferCredit)
    }

    @Test
    fun parcelsCorrectly() {
        val request = PayPalVaultRequest()
        request.localeCode = "en-US"
        request.billingAgreementDescription = "Billing Agreement Description"
        request.isShippingAddressRequired = true
        request.isShippingAddressEditable = true
        request.shouldOfferCredit = true

        val postalAddress = PostalAddress()
        postalAddress.recipientName = "Postal Address"
        request.shippingAddressOverride = postalAddress
        request.landingPageType = PayPalRequest.LANDING_PAGE_TYPE_LOGIN
        request.displayName = "Display Name"
        request.merchantAccountId = "merchant_account_id"

        val lineItems = ArrayList<PayPalLineItem>()
        lineItems.add(PayPalLineItem(PayPalLineItem.KIND_DEBIT, "An Item", "1", "1"))
        request.setLineItems(lineItems)

        val parcel = Parcel.obtain()
        request.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val result = PayPalVaultRequest.CREATOR.createFromParcel(parcel)

        assertEquals("en-US", result.localeCode)
        assertEquals("Billing Agreement Description",
                result.billingAgreementDescription)
        assertTrue(result.shouldOfferCredit)
        assertTrue(result.isShippingAddressRequired)
        assertTrue(result.isShippingAddressEditable)
        assertEquals("Postal Address", result.shippingAddressOverride?.recipientName)
        assertEquals(PayPalRequest.LANDING_PAGE_TYPE_LOGIN, result.landingPageType)
        assertEquals("Display Name", result.displayName)
        assertEquals("merchant_account_id", result.merchantAccountId)
        assertEquals(1, result.lineItems.size)
        assertEquals("An Item", result.lineItems[0].name)
    }
}