package com.braintreepayments.api.uicomponents.cardfields

import com.braintreepayments.api.uicomponents.R
import org.junit.Assert.assertEquals
import org.junit.Test

class CardNumberValidationUseCaseUnitTest {

    private val sut = CardNumberValidationUseCase()

    @Test
    fun `valid Visa number returns Valid with VISA brand`() {
        val result = sut("4111111111111111")
        assertEquals(ValidationResult.Valid, result.validation)
        assertEquals(CardBrand.VISA, result.detectedBrand)
    }

    @Test
    fun `valid Amex number returns Valid with AMEX brand`() {
        val result = sut("378282246310005")
        assertEquals(ValidationResult.Valid, result.validation)
        assertEquals(CardBrand.AMEX, result.detectedBrand)
    }

    @Test
    fun `valid Mastercard number returns Valid with MASTERCARD brand`() {
        val result = sut("5555555555554444")
        assertEquals(ValidationResult.Valid, result.validation)
        assertEquals(CardBrand.MASTERCARD, result.detectedBrand)
    }

    @Test
    fun `valid Discover number returns Valid with DISCOVER brand`() {
        val result = sut("6011111111111117")
        assertEquals(ValidationResult.Valid, result.validation)
        assertEquals(CardBrand.DISCOVER, result.detectedBrand)
    }

    @Test
    fun `valid Diners Club number returns Valid`() {
        val result = sut("30569309025904")
        assertEquals(ValidationResult.Valid, result.validation)
        assertEquals(CardBrand.DINERS_CLUB, result.detectedBrand)
    }

    @Test
    fun `valid JCB number returns Valid`() {
        val result = sut("3530111333300000")
        assertEquals(ValidationResult.Valid, result.validation)
        assertEquals(CardBrand.JCB, result.detectedBrand)
    }

    @Test
    fun `invalid Luhn returns Invalid with correct brand`() {
        val result = sut("4111111111111112")
        assertEquals(ValidationResult.Invalid(R.string.card_number_error), result.validation)
        assertEquals(CardBrand.VISA, result.detectedBrand)
    }

    @Test
    fun `partial input below minLength returns Validating`() {
        val result = sut("411111111111")
        assertEquals(ValidationResult.Validating, result.validation)
        assertEquals(CardBrand.VISA, result.detectedBrand)
    }

    @Test
    fun `empty string returns Validating with UNKNOWN brand`() {
        val result = sut("")
        assertEquals(ValidationResult.Validating, result.validation)
        assertEquals(CardBrand.UNKNOWN, result.detectedBrand)
    }

    @Test
    fun `UnionPay skips Luhn validation when length is valid`() {
        val result = sut("6240000000000000")
        assertEquals(ValidationResult.Valid, result.validation)
        assertEquals(CardBrand.UNIONPAY, result.detectedBrand)
    }

    @Test
    fun `UnionPay with partial input returns Validating`() {
        val result = sut("624000000000")
        assertEquals(ValidationResult.Validating, result.validation)
        assertEquals(CardBrand.UNIONPAY, result.detectedBrand)
    }

    @Test
    fun `ambiguous prefix resolves to UNKNOWN brand`() {
        // "627780" matches both ELO and UNIONPAY prefix patterns
        val result = sut("6277801234567890")
        assertEquals(CardBrand.UNKNOWN, result.detectedBrand)
    }
}
