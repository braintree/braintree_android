package com.braintreepayments.api.uicomponents.cardfields

import org.junit.Assert.assertEquals
import org.junit.Test

class ExpirationDateFormatterUnitTest {

    @Test
    fun `applyLeadingZero returns digit unchanged when it is 0`() {
        assertEquals("0", ExpirationDateFormatter.applyLeadingZero("0"))
    }

    @Test
    fun `applyLeadingZero returns digit unchanged when it is 1`() {
        assertEquals("1", ExpirationDateFormatter.applyLeadingZero("1"))
    }

    @Test
    fun `applyLeadingZero prefixes 2 through 9 with leading zero`() {
        for (d in '2'..'9') {
            assertEquals("0$d", ExpirationDateFormatter.applyLeadingZero(d.toString()))
        }
    }

    @Test
    fun `isValidMonth returns true for incomplete input`() {
        assertEquals(true, ExpirationDateFormatter.isValidMonth(""))
        assertEquals(true, ExpirationDateFormatter.isValidMonth("0"))
        assertEquals(true, ExpirationDateFormatter.isValidMonth("1"))
    }

    @Test
    fun `isValidMonth returns true for valid months 01 through 12`() {
        for (month in 1..12) {
            val digits = month.toString().padStart(2, '0')
            assertEquals(true, ExpirationDateFormatter.isValidMonth(digits))
        }
    }

    @Test
    fun `isValidMonth returns false for month 00`() {
        assertEquals(false, ExpirationDateFormatter.isValidMonth("00"))
    }

    @Test
    fun `isValidMonth returns false for months 13 through 19`() {
        for (month in 13..19) {
            assertEquals(false, ExpirationDateFormatter.isValidMonth(month.toString()))
        }
    }

    @Test
    fun `formatExpiration returns empty string for empty input`() {
        assertEquals("", ExpirationDateFormatter.formatExpiration(""))
    }

    @Test
    fun `formatExpiration returns single digit unchanged`() {
        assertEquals("1", ExpirationDateFormatter.formatExpiration("1"))
    }

    @Test
    fun `formatExpiration returns two digits unchanged`() {
        assertEquals("12", ExpirationDateFormatter.formatExpiration("12"))
    }

    @Test
    fun `formatExpiration inserts slash after two digits`() {
        assertEquals("12/3", ExpirationDateFormatter.formatExpiration("123"))
    }

    @Test
    fun `formatExpiration formats four digits as MM slash YY`() {
        assertEquals("12/34", ExpirationDateFormatter.formatExpiration("1234"))
    }

    @Test
    fun `findCursorPosition returns 0 for digitIndex 0`() {
        assertEquals(0, ExpirationDateFormatter.findCursorPosition("12/34", 0))
    }

    @Test
    fun `findCursorPosition returns correct index before slash`() {
        assertEquals(1, ExpirationDateFormatter.findCursorPosition("12/34", 1))
        assertEquals(2, ExpirationDateFormatter.findCursorPosition("12/34", 2))
    }

    @Test
    fun `findCursorPosition skips slash when counting`() {
        assertEquals(4, ExpirationDateFormatter.findCursorPosition("12/34", 3))
        assertEquals(5, ExpirationDateFormatter.findCursorPosition("12/34", 4))
    }

    @Test
    fun `findCursorPosition returns end of string when digitIndex exceeds digits`() {
        assertEquals(5, ExpirationDateFormatter.findCursorPosition("12/34", 99))
    }
}
