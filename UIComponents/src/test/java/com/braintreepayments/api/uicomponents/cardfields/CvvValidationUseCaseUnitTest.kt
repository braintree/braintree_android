package com.braintreepayments.api.uicomponents.cardfields

import com.braintreepayments.api.uicomponents.R
import org.junit.Assert.assertEquals
import org.junit.Test

class CvvValidationUseCaseUnitTest {

    private val sut = CvvValidationUseCase()

    @Test
    fun `3-digit CVV with Visa returns Valid`() {
        assertEquals(ValidationResult.Valid, sut("123", CardBrand.VISA))
    }

    @Test
    fun `4-digit CVV with Amex returns Valid`() {
        assertEquals(ValidationResult.Valid, sut("1234", CardBrand.AMEX))
    }

    @Test
    fun `3-digit CVV with Amex returns Validating`() {
        assertEquals(ValidationResult.Validating, sut("123", CardBrand.AMEX))
    }

    @Test
    fun `4-digit CVV with Visa returns Invalid`() {
        assertEquals(ValidationResult.Invalid(R.string.cvv_error), sut("1234", CardBrand.VISA))
    }

    @Test
    fun `empty CVV returns Validating`() {
        assertEquals(ValidationResult.Validating, sut("", CardBrand.VISA))
    }

    @Test
    fun `2-digit CVV returns Validating`() {
        assertEquals(ValidationResult.Validating, sut("12", CardBrand.VISA))
    }

    @Test
    fun `3-digit CVV with UNKNOWN brand returns Valid`() {
        assertEquals(ValidationResult.Valid, sut("123", CardBrand.UNKNOWN))
    }

    @Test
    fun `4-digit CVV with UNKNOWN brand returns Valid`() {
        assertEquals(ValidationResult.Valid, sut("1234", CardBrand.UNKNOWN))
    }

    @Test
    fun `2-digit CVV with UNKNOWN brand returns Validating`() {
        assertEquals(ValidationResult.Validating, sut("12", CardBrand.UNKNOWN))
    }

    @Test
    fun `3-digit CVV with Mastercard returns Valid`() {
        assertEquals(ValidationResult.Valid, sut("456", CardBrand.MASTERCARD))
    }
}
