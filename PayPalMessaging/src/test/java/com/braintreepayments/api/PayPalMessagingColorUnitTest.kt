package com.braintreepayments.api

import com.paypal.messages.config.message.style.PayPalMessageColor
import junit.framework.TestCase.assertEquals
import org.junit.Test

class PayPalMessagingColorUnitTest {

    @Test
    fun testColor_withBlack_returnsRawValueBlack() {
        assertEquals(PayPalMessageColor.BLACK, PayPalMessagingColor.BLACK.messageColorRawValue)
    }

    @Test
    fun testColor_withWhite_returnsRawValueWhite() {
        assertEquals(PayPalMessageColor.WHITE, PayPalMessagingColor.WHITE.messageColorRawValue)
    }

    @Test
    fun testColor_withMonochrome_returnsRawValueMonochrome() {
        assertEquals(PayPalMessageColor.MONOCHROME, PayPalMessagingColor.MONOCHROME.messageColorRawValue)
    }

    @Test
    fun testColor_withGreyscale_returnsRawValueGreyscale() {
        assertEquals(PayPalMessageColor.GRAYSCALE, PayPalMessagingColor.GRAYSCALE.messageColorRawValue)
    }
}
