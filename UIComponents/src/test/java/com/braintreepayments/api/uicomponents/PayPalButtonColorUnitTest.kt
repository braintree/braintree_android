package com.braintreepayments.api.uicomponents

import org.junit.Assert.assertSame
import org.junit.Test

class PayPalButtonColorUnitTest {

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
}
