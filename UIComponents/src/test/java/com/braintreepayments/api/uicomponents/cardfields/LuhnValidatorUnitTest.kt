package com.braintreepayments.api.uicomponents.cardfields

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LuhnValidatorUnitTest {

    @Test
    fun `isLuhnValid returns true for valid Visa number`() {
        assertTrue(LuhnValidator.isLuhnValid("4111111111111111"))
    }

    @Test
    fun `isLuhnValid returns true for valid Amex number`() {
        assertTrue(LuhnValidator.isLuhnValid("378282246310005"))
    }

    @Test
    fun `isLuhnValid returns true for valid Mastercard number`() {
        assertTrue(LuhnValidator.isLuhnValid("5555555555554444"))
    }

    @Test
    fun `isLuhnValid returns true for valid Discover number`() {
        assertTrue(LuhnValidator.isLuhnValid("6011111111111117"))
    }

    @Test
    fun `isLuhnValid returns true for valid Diners Club number`() {
        assertTrue(LuhnValidator.isLuhnValid("30569309025904"))
    }

    @Test
    fun `isLuhnValid returns true for valid JCB number`() {
        assertTrue(LuhnValidator.isLuhnValid("3530111333300000"))
    }

    @Test
    fun `isLuhnValid returns false for invalid Visa number`() {
        assertFalse(LuhnValidator.isLuhnValid("4111111111111112"))
    }

    @Test
    fun `isLuhnValid returns false for number with single digit changed`() {
        assertFalse(LuhnValidator.isLuhnValid("4111111111111110"))
    }

    @Test
    fun `isLuhnValid returns true for single digit zero`() {
        assertTrue(LuhnValidator.isLuhnValid("0"))
    }

    @Test
    fun `isLuhnValid returns true for empty string`() {
        assertTrue(LuhnValidator.isLuhnValid(""))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `isLuhnValid throws for non-digit character`() {
        LuhnValidator.isLuhnValid("4111111111111a11")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `isLuhnValid throws for spaces in input`() {
        LuhnValidator.isLuhnValid("4111 1111 1111 1111")
    }

    @Test
    fun `isLuhnValid returns true for second valid Visa number`() {
        assertTrue(LuhnValidator.isLuhnValid("4005519200000004"))
    }

    @Test
    fun `isLuhnValid returns false for invalid Amex number`() {
        assertFalse(LuhnValidator.isLuhnValid("371111111111111"))
    }
}
