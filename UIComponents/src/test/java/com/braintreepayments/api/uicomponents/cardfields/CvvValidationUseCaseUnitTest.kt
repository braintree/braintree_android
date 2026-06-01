package com.braintreepayments.api.uicomponents.cardfields

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CvvValidationUseCaseUnitTest {

    private val sut = CvvValidationUseCase()

    @Test
    fun `3-digit CVV with Visa returns true`() {
        assertTrue(sut("123", CardBrand.VISA))
    }

    @Test
    fun `4-digit CVV with Amex returns true`() {
        assertTrue(sut("1234", CardBrand.AMEX))
    }

    @Test
    fun `3-digit CVV with Amex returns false`() {
        assertFalse(sut("123", CardBrand.AMEX))
    }

    @Test
    fun `4-digit CVV with Visa returns false`() {
        assertFalse(sut("1234", CardBrand.VISA))
    }

    @Test
    fun `empty CVV returns false`() {
        assertFalse(sut("", CardBrand.VISA))
    }

    @Test
    fun `2-digit CVV returns false`() {
        assertFalse(sut("12", CardBrand.VISA))
    }

    @Test
    fun `3-digit CVV with UNKNOWN brand returns true`() {
        assertTrue(sut("123", CardBrand.UNKNOWN))
    }

    @Test
    fun `4-digit CVV with UNKNOWN brand returns false`() {
        assertFalse(sut("1234", CardBrand.UNKNOWN))
    }

    @Test
    fun `3-digit CVV with Mastercard returns true`() {
        assertTrue(sut("456", CardBrand.MASTERCARD))
    }
}
