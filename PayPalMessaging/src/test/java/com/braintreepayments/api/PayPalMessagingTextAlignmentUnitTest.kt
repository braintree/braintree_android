package com.braintreepayments.api

import com.paypal.messages.config.message.style.PayPalMessageAlign
import junit.framework.TestCase.assertEquals
import org.junit.Test

class PayPalMessagingTextAlignmentUnitTest {
    @Test
    fun testTextAlignment_withLeft_returnsRawValueLeft() {
        assertEquals(PayPalMessagingTextAlignment.LEFT.textAlignmentRawValue, PayPalMessageAlign.LEFT)
    }

    @Test
    fun testTextAlignment_withCenter_returnsRawValueCenter() {
        assertEquals(PayPalMessagingTextAlignment.RIGHT.textAlignmentRawValue, PayPalMessageAlign.RIGHT)
    }

    @Test
    fun testTextAlignment_withRight_returnsRawValueRight() {
        assertEquals(PayPalMessagingTextAlignment.CENTER.textAlignmentRawValue, PayPalMessageAlign.CENTER)
    }
}