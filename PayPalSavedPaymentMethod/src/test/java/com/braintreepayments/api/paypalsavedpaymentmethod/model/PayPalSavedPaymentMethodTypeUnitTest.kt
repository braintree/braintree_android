package com.braintreepayments.api.paypalsavedpaymentmethod.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PayPalSavedPaymentMethodTypeUnitTest {

    @Test
    fun `fromRawValue returns the matching type for each raw value`() {
        assertEquals(PayPalSavedPaymentMethodType.BANK, PayPalSavedPaymentMethodType.fromRawValue("BANK"))
        assertEquals(PayPalSavedPaymentMethodType.CARD, PayPalSavedPaymentMethodType.fromRawValue("CARD"))
        assertEquals(
            PayPalSavedPaymentMethodType.PAYPAL_CREDIT,
            PayPalSavedPaymentMethodType.fromRawValue("PAYPAL_CREDIT")
        )
    }

    @Test
    fun `fromRawValue returns null for an unrecognized type`() {
        assertNull(PayPalSavedPaymentMethodType.fromRawValue("SOMETHING_NEW"))
    }

    @Test
    fun `fromRawValue returns null for null`() {
        assertNull(PayPalSavedPaymentMethodType.fromRawValue(null))
    }
}
