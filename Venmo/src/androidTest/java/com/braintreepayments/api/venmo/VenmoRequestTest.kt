package com.braintreepayments.api.venmo

import android.os.Parcel
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import kotlinx.parcelize.parcelableCreator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class VenmoRequestTest {

    @Test
    fun parcelize_withAllFieldsPopulated_restoresAllFields() {
        val lineItem = VenmoLineItem(
            kind = VenmoLineItemKind.DEBIT,
            name = "Widget",
            quantity = 2,
            unitAmount = "10.00",
            description = "A widget",
            productCode = "SKU-001",
            unitTaxAmount = "1.00",
            url = "https://example.com/widget"
        )
        val original = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.MULTI_USE,
            lineItems = arrayListOf(lineItem),
            shouldVault = true,
            profileId = "profile-123",
            displayName = "My Store",
            collectCustomerShippingAddress = true,
            collectCustomerBillingAddress = true,
            totalAmount = "100.00",
            subTotalAmount = "90.00",
            discountAmount = "5.00",
            taxAmount = "8.00",
            shippingAmount = "2.00",
            isFinalAmount = true,
            riskCorrelationId = "risk-abc"
        )

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val restored = parcelableCreator<VenmoRequest>().createFromParcel(parcel)
        parcel.recycle()

        assertEquals(VenmoPaymentMethodUsage.MULTI_USE, restored.paymentMethodUsage)
        assertTrue(restored.shouldVault)
        assertEquals("profile-123", restored.profileId)
        assertEquals("My Store", restored.displayName)
        assertTrue(restored.collectCustomerShippingAddress)
        assertTrue(restored.collectCustomerBillingAddress)
        assertEquals("100.00", restored.totalAmount)
        assertEquals("90.00", restored.subTotalAmount)
        assertEquals("5.00", restored.discountAmount)
        assertEquals("8.00", restored.taxAmount)
        assertEquals("2.00", restored.shippingAmount)
        assertTrue(restored.isFinalAmount)
        assertEquals("risk-abc", restored.riskCorrelationId)
        assertNotNull(restored.lineItems)
        assertEquals(1, restored.lineItems!!.size)
        assertEquals("Widget", restored.lineItems!![0].name)
    }

    @Test
    fun parcelize_withDefaultValues_restoresDefaults() {
        val original = VenmoRequest(paymentMethodUsage = VenmoPaymentMethodUsage.SINGLE_USE)

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        val restored = parcelableCreator<VenmoRequest>().createFromParcel(parcel)
        parcel.recycle()

        assertEquals(VenmoPaymentMethodUsage.SINGLE_USE, restored.paymentMethodUsage)
        assertFalse(restored.shouldVault)
        assertNull(restored.profileId)
        assertNull(restored.displayName)
        assertFalse(restored.collectCustomerShippingAddress)
        assertFalse(restored.collectCustomerBillingAddress)
        assertNull(restored.totalAmount)
        assertNull(restored.subTotalAmount)
        assertNull(restored.discountAmount)
        assertNull(restored.taxAmount)
        assertNull(restored.shippingAmount)
        assertFalse(restored.isFinalAmount)
        assertNull(restored.riskCorrelationId)
        assertNull(restored.lineItems)
    }
}
