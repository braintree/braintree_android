package com.braintreepayments.api.uicomponents.cardfields

import com.braintreepayments.api.uicomponents.R
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpirationValidationUseCaseUnitTest {

    private val sut = ExpirationValidationUseCase()

    @Test
    fun `valid future date returns Valid`() {
        assertEquals(ValidationResult.Valid, sut("1228", currentMonth = 6, currentYear = 2026))
    }

    @Test
    fun `current month and year returns Valid`() {
        assertEquals(ValidationResult.Valid, sut("0626", currentMonth = 6, currentYear = 2026))
    }

    @Test
    fun `past month in current year returns Invalid`() {
        assertEquals(
            ValidationResult.Invalid(R.string.expiration_error),
            sut("0326", currentMonth = 6, currentYear = 2026)
        )
    }

    @Test
    fun `past year returns Invalid`() {
        assertEquals(
            ValidationResult.Invalid(R.string.expiration_error),
            sut("1225", currentMonth = 6, currentYear = 2026)
        )
    }

    @Test
    fun `month 00 returns Invalid`() {
        assertEquals(
            ValidationResult.Invalid(R.string.expiration_error),
            sut("0028", currentMonth = 1, currentYear = 2026)
        )
    }

    @Test
    fun `month 13 returns Invalid`() {
        assertEquals(
            ValidationResult.Invalid(R.string.expiration_error),
            sut("1328", currentMonth = 1, currentYear = 2026)
        )
    }

    @Test
    fun `too few digits returns Validating`() {
        assertEquals(ValidationResult.Validating, sut("012", currentMonth = 1, currentYear = 2026))
    }

    @Test
    fun `too many digits returns Invalid`() {
        assertEquals(
            ValidationResult.Invalid(R.string.expiration_error),
            sut("01268", currentMonth = 1, currentYear = 2026)
        )
    }

    @Test
    fun `empty string returns Validating`() {
        assertEquals(ValidationResult.Validating, sut("", currentMonth = 1, currentYear = 2026))
    }

    @Test
    fun `January of next year returns Valid`() {
        assertEquals(ValidationResult.Valid, sut("0127", currentMonth = 12, currentYear = 2026))
    }

    @Test
    fun `December of current year with current month January returns Valid`() {
        assertEquals(ValidationResult.Valid, sut("1226", currentMonth = 1, currentYear = 2026))
    }
}
