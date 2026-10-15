@file:OptIn(ExperimentalBetaApi::class)

package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PayPalSavedPaymentMethodRequestUnitTest {

    @Test
    fun `constructor sets the amount, currency code and merchant account`() {
        val request = PayPalSavedPaymentMethodRequest("55.00", "USD", "merchant-account-1")

        assertEquals("55.00", request.amount)
        assertEquals("USD", request.currencyCode)
        assertEquals("merchant-account-1", request.merchantAccountId)
    }

    @Test
    fun `merchantAccountId defaults to null`() {
        assertNull(PayPalSavedPaymentMethodRequest("55.00", "USD").merchantAccountId)
    }
}
