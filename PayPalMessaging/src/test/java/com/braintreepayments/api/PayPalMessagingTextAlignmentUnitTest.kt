package com.braintreepayments.api

import com.paypal.messages.config.message.style.PayPalMessageAlign
import junit.framework.TestCase.assertEquals
import org.junit.Test

class PayPalMessagingTextAlignmentUnitTest {

    @Test
    fun testTextAlignment_withLeft_returnsRawValueLeft() {
        assertEquals(PayPalMessageAlign.LEFT, PayPalMessagingTextAlignment.LEFT.textAlignmentRawValue)
    }

    @Test
    fun testTextAlignment_withCenter_returnsRawValueCenter() {
        assertEquals(PayPalMessageAlign.RIGHT, PayPalMessagingTextAlignment.RIGHT.textAlignmentRawValue)
    }

    @Test
    fun testTextAlignment_withRight_returnsRawValueRight() {
        assertEquals(PayPalMessageAlign.CENTER, PayPalMessagingTextAlignment.CENTER.textAlignmentRawValue)
    }
}