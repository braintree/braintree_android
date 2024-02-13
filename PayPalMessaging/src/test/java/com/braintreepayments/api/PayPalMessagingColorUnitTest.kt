package com.braintreepayments.api

import com.paypal.messages.config.message.style.PayPalMessageColor
import junit.framework.TestCase.assertEquals
import org.junit.Test

class PayPalMessagingColorUnitTest {

    @Test
    fun testColor_withBlack_returnsRawValueBlack() {
        assertEquals(PayPalMessagingColor.BLACK.messageColorRawValue, PayPalMessageColor.BLACK)
    }

    @Test
    fun testColor_withWhite_returnsRawValueWhite() {
        assertEquals(PayPalMessagingColor.WHITE.messageColorRawValue, PayPalMessageColor.WHITE)
    }

    @Test
    fun testColor_withMonochrome_returnsRawValueMonochrome() {
        assertEquals(PayPalMessagingColor.MONOCHROME.messageColorRawValue, PayPalMessageColor.MONOCHROME)
    }

    @Test
    fun testColor_withGreyscale_returnsRawValueGreyscale() {
        assertEquals(PayPalMessagingColor.GRAYSCALE.messageColorRawValue, PayPalMessageColor.GRAYSCALE)
    }
}
