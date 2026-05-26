package com.braintreepayments.api.uicomponents.cardfields

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardNumberValidationUseCaseUnitTest {

    private val sut = CardNumberValidationUseCase()

    @Test
    fun `valid Visa number returns isValid true with VISA brand`() {
        val result = sut("4111111111111111")
        assertTrue(result.isValid)
        assertEquals(CardBrand.VISA, result.detectedBrand)
    }

    @Test
    fun `valid Amex number returns isValid true with AMEX brand`() {
        val result = sut("378282246310005")
        assertTrue(result.isValid)
        assertEquals(CardBrand.AMEX, result.detectedBrand)
    }

    @Test
    fun `valid Mastercard number returns isValid true with MASTERCARD brand`() {
        val result = sut("5555555555554444")
        assertTrue(result.isValid)
        assertEquals(CardBrand.MASTERCARD, result.detectedBrand)
    }

    @Test
    fun `valid Discover number returns isValid true with DISCOVER brand`() {
        val result = sut("6011111111111117")
        assertTrue(result.isValid)
        assertEquals(CardBrand.DISCOVER, result.detectedBrand)
    }

    @Test
    fun `valid Diners Club number returns isValid true`() {
        val result = sut("30569309025904")
        assertTrue(result.isValid)
        assertEquals(CardBrand.DINERS_CLUB, result.detectedBrand)
    }

    @Test
    fun `valid JCB number returns isValid true`() {
        val result = sut("3530111333300000")
        assertTrue(result.isValid)
        assertEquals(CardBrand.JCB, result.detectedBrand)
    }

    @Test
    fun `invalid Luhn returns isValid false with correct brand`() {
        val result = sut("4111111111111112")
        assertFalse(result.isValid)
        assertEquals(CardBrand.VISA, result.detectedBrand)
    }

    @Test
    fun `wrong length for detected brand returns isValid false`() {
        val result = sut("411111111111")
        assertFalse(result.isValid)
        assertEquals(CardBrand.VISA, result.detectedBrand)
    }

    @Test
    fun `empty string returns isValid false with UNKNOWN brand`() {
        val result = sut("")
        assertFalse(result.isValid)
        assertEquals(CardBrand.UNKNOWN, result.detectedBrand)
    }

    @Test
    fun `UnionPay skips Luhn validation when length is valid`() {
        val result = sut("6240000000000000")
        assertTrue(result.isValid)
        assertEquals(CardBrand.UNIONPAY, result.detectedBrand)
    }

    @Test
    fun `UnionPay with invalid length returns isValid false`() {
        val result = sut("624000000000")
        assertFalse(result.isValid)
        assertEquals(CardBrand.UNIONPAY, result.detectedBrand)
    }

    @Test
    fun `ambiguous prefix resolves to UNKNOWN brand`() {
        // "627780" matches both ELO and UNIONPAY prefix patterns
        val result = sut("6277801234567890")
        assertEquals(CardBrand.UNKNOWN, result.detectedBrand)
    }
}
