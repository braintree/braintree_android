package com.braintreepayments.api.paypal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PayPalPaymentIntentTest {

    @Test
    fun `Test PayPalPaymentIntent fromString -- accepts case insensitive strings`() {
        assertEquals(PayPalPaymentIntent.ORDER, PayPalPaymentIntent.fromString("order"))
        assertEquals(PayPalPaymentIntent.ORDER, PayPalPaymentIntent.fromString("Order"))
        assertEquals(PayPalPaymentIntent.ORDER, PayPalPaymentIntent.fromString("ORDER"))
    }

    @Test
    fun `Test PayPalPaymentIntent fromString - random string fails equality`() {
        assertNotEquals(PayPalPaymentIntent.ORDER, PayPalPaymentIntent.fromString("random"))
    }
}
