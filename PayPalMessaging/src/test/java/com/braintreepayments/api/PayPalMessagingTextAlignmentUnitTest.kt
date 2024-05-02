package com.braintreepayments.api

import com.paypal.messages.config.message.style.PayPalMessageAlign
import junit.framework.TestCase.assertEquals
import org.junit.Test

class PayPalMessagingTextAlignmentUnitTest {

    @Test
    fun `test left text alignment returns raw value left align`() {
        assertEquals(PayPalMessageAlign.LEFT, PayPalMessagingTextAlignment.LEFT.internalValue)
    }

    @Test
    fun `test right text alignment returns raw value right align`() {
        assertEquals(PayPalMessageAlign.RIGHT, PayPalMessagingTextAlignment.RIGHT.internalValue)
    }

    @Test
    fun `test center text alignment returns raw value center align`() {
        assertEquals(PayPalMessageAlign.CENTER, PayPalMessagingTextAlignment.CENTER.internalValue)
    }
}
