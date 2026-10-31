package com.braintreepayments.api.uicomponents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class VenmoButtonColorUnitTest {

    @Test
    fun `keys match attribute ids`() {
        assertEquals(0, VenmoButtonColor.Blue.key)
        assertEquals(1, VenmoButtonColor.Black.key)
        assertEquals(2, VenmoButtonColor.White.key)
    }

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

    @Test
    fun `focus colors include venmo focus indicator`() {
        listOf(VenmoButtonColor.Blue, VenmoButtonColor.Black, VenmoButtonColor.White).forEach {
            assertEquals(R.color.venmo_focus_indicator, it.focus.focusIndicator)
            assertEquals(R.color.venmo_focus_indicator, it.focusHover.focusIndicator)
        }
    }
}
