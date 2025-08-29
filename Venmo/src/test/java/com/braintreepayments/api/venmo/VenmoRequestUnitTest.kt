package com.braintreepayments.api.venmo

import android.os.Parcel
import kotlinx.parcelize.parcelableCreator
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class VenmoRequestUnitTest {

    @Test
    fun `parcels correctly`() {
        val lineItems = ArrayList<VenmoLineItem>()
        lineItems.add(VenmoLineItem(VenmoLineItemKind.DEBIT, "An Item", 1, "10.00"))

        val sut = VenmoRequest(
            paymentMethodUsage = VenmoPaymentMethodUsage.MULTI_USE,
            displayName = "venmo-user",
            shouldVault = true,
            profileId = "profile-id",
            collectCustomerBillingAddress = true,
            collectCustomerShippingAddress = true,
            subTotalAmount = "10.00",
            taxAmount = "1.00",
            discountAmount = "2.00",
            shippingAmount = "1.00",
            totalAmount = "10.00",
            isFinalAmount = true,
            lineItems = lineItems
        )

        val parcel = Parcel.obtain().apply {
            sut.writeToParcel(this, 0)
            setDataPosition(0)
        }
        val result = parcelableCreator<VenmoRequest>().createFromParcel(parcel)

        assertEquals(VenmoPaymentMethodUsage.MULTI_USE, result.paymentMethodUsage)
        assertEquals("venmo-user", result.displayName)
        assertTrue { result.shouldVault }
        assertEquals("profile-id", result.profileId)
        assertTrue(result.collectCustomerBillingAddress)
        assertTrue(result.collectCustomerShippingAddress)
        assertEquals("10.00", result.subTotalAmount)
        assertEquals("1.00", result.taxAmount)
        assertEquals("2.00", result.discountAmount)
        assertEquals("1.00", result.shippingAmount)
        assertEquals("10.00", result.totalAmount)
        assertEquals(1, result.lineItems?.size)
        assertEquals("An Item", result.lineItems?.get(0)?.name)
        assertTrue(result.isFinalAmount)
    }
}
