package com.braintreepayments.api.uicomponents.cardfields

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpirationValidationUseCaseUnitTest {

    private val sut = ExpirationValidationUseCase()

    @Test
    fun `valid future date returns true`() {
        assertTrue(sut("1228", currentMonth = 6, currentYear = 2026))
    }

    @Test
    fun `current month and year returns true`() {
        assertTrue(sut("0626", currentMonth = 6, currentYear = 2026))
    }

    @Test
    fun `past month in current year returns false`() {
        assertFalse(sut("0326", currentMonth = 6, currentYear = 2026))
    }

    @Test
    fun `past year returns false`() {
        assertFalse(sut("1225", currentMonth = 6, currentYear = 2026))
    }

    @Test
    fun `month 00 returns false`() {
        assertFalse(sut("0028", currentMonth = 1, currentYear = 2026))
    }

    @Test
    fun `month 13 returns false`() {
        assertFalse(sut("1328", currentMonth = 1, currentYear = 2026))
    }

    @Test
    fun `too few digits returns false`() {
        assertFalse(sut("012", currentMonth = 1, currentYear = 2026))
    }

    @Test
    fun `too many digits returns false`() {
        assertFalse(sut("01268", currentMonth = 1, currentYear = 2026))
    }

    @Test
    fun `empty string returns false`() {
        assertFalse(sut("", currentMonth = 1, currentYear = 2026))
    }

    @Test
    fun `January of next year returns true`() {
        assertTrue(sut("0127", currentMonth = 12, currentYear = 2026))
    }

    @Test
    fun `December of current year with current month January returns true`() {
        assertTrue(sut("1226", currentMonth = 1, currentYear = 2026))
    }
}