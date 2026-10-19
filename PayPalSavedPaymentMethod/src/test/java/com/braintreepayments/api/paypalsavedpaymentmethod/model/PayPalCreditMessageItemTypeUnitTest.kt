package com.braintreepayments.api.paypalsavedpaymentmethod.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PayPalCreditMessageItemTypeUnitTest {

    @Test
    fun `fromRawValue returns the matching type for each raw value`() {
        assertEquals(PayPalCreditMessageItemType.IMAGE, PayPalCreditMessageItemType.fromRawValue("IMAGE"))
        assertEquals(PayPalCreditMessageItemType.LINK, PayPalCreditMessageItemType.fromRawValue("LINK"))
        assertEquals(PayPalCreditMessageItemType.TEXT, PayPalCreditMessageItemType.fromRawValue("TEXT"))
    }

    @Test
    fun `fromRawValue returns null for an unrecognized type`() {
        assertNull(PayPalCreditMessageItemType.fromRawValue("TEXT_VARIABLE"))
    }

    @Test
    fun `fromRawValue returns null for null`() {
        assertNull(PayPalCreditMessageItemType.fromRawValue(null))
    }
}
