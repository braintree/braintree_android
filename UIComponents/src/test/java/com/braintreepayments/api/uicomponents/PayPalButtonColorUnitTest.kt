package com.braintreepayments.api.uicomponents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PayPalButtonColorUnitTest {

    @Test
    fun `keys match attribute ids`() {
        assertEquals(0, PayPalButtonColor.Blue.key)
        assertEquals(1, PayPalButtonColor.Black.key)
        assertEquals(2, PayPalButtonColor.White.key)
    }

    @Test
    fun `fromId returns matching color`() {
        assertSame(PayPalButtonColor.Blue, PayPalButtonColor.fromId(0))
        assertSame(PayPalButtonColor.Black, PayPalButtonColor.fromId(1))
        assertSame(PayPalButtonColor.White, PayPalButtonColor.fromId(2))
    }

    @Test
    fun `fromId falls back to Blue for unknown id`() {
        assertSame(PayPalButtonColor.Blue, PayPalButtonColor.fromId(99))
        assertSame(PayPalButtonColor.Blue, PayPalButtonColor.fromId(-1))
    }

    @Test
    fun `focus colors include focus indicator and default does not`() {
        listOf(PayPalButtonColor.Blue, PayPalButtonColor.Black, PayPalButtonColor.White).forEach {
            assertEquals(R.color.paypal_color_transparent, it.default.focusIndicator)
            assertEquals(R.color.paypal_focus_indicator, it.focus.focusIndicator)
            assertEquals(R.color.paypal_focus_indicator, it.focusHover.focusIndicator)
        }
    }
}
