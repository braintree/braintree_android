package com.braintreepayments.api.uicomponents

import org.junit.Assert.assertSame
import org.junit.Test

class VenmoButtonColorUnitTest {

    @Test
    fun `fromId returns matching color`() {
        assertSame(VenmoButtonColor.Blue, VenmoButtonColor.fromId(0))
        assertSame(VenmoButtonColor.Black, VenmoButtonColor.fromId(1))
        assertSame(VenmoButtonColor.White, VenmoButtonColor.fromId(2))
    }

    @Test
    fun `fromId falls back to Blue for unknown id`() {
        assertSame(VenmoButtonColor.Blue, VenmoButtonColor.fromId(99))
        assertSame(VenmoButtonColor.Blue, VenmoButtonColor.fromId(-1))
    }
}
