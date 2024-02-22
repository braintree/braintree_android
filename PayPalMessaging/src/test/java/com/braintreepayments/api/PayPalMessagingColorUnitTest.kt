package com.braintreepayments.api

import com.paypal.messages.config.message.style.PayPalMessageColor
import junit.framework.TestCase.assertEquals
import org.junit.Test

class PayPalMessagingColorUnitTest {

    @Test
    fun `test black color returns raw value black`() {
        assertEquals(PayPalMessageColor.BLACK, PayPalMessagingColor.BLACK.messageColorRawValue)
    }

    @Test
    fun `test white color returns raw value white`() {
        assertEquals(PayPalMessageColor.WHITE, PayPalMessagingColor.WHITE.messageColorRawValue)
    }

    @Test
    fun `test monochrome color returns raw value monochrome`() {
        assertEquals(PayPalMessageColor.MONOCHROME, PayPalMessagingColor.MONOCHROME.messageColorRawValue)
    }

    @Test
    fun `test greyscale color returns raw value greyscale`() {
        assertEquals(PayPalMessageColor.GRAYSCALE, PayPalMessagingColor.GRAYSCALE.messageColorRawValue)
    }
}
