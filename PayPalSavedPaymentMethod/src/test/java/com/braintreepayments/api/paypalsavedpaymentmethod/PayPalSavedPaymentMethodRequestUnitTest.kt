@file:OptIn(ExperimentalBetaApi::class)

package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PayPalSavedPaymentMethodRequestUnitTest {

    private val request = PayPalSavedPaymentMethodRequest("55.00", "USD", "merchant-account-1")

    @Test
    fun `requests with the same values are equal and have the same hash code`() {
        val sameValues = PayPalSavedPaymentMethodRequest("55.00", "USD", "merchant-account-1")

        assertEquals(request, sameValues)
        assertEquals(request.hashCode(), sameValues.hashCode())
    }

    @Test
    fun `requests that differ in any value are not equal`() {
        assertNotEquals(request, PayPalSavedPaymentMethodRequest("70.00", "USD", "merchant-account-1"))
        assertNotEquals(request, PayPalSavedPaymentMethodRequest("55.00", "EUR", "merchant-account-1"))
        assertNotEquals(request, PayPalSavedPaymentMethodRequest("55.00", "USD", "merchant-account-2"))
        assertNotEquals(request, PayPalSavedPaymentMethodRequest("55.00", "USD"))
    }
}
