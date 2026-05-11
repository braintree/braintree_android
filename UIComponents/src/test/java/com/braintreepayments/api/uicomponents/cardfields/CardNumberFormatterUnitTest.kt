package com.braintreepayments.api.uicomponents.cardfields

import android.text.SpannableStringBuilder
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CardNumberFormatterUnitTest {

    @Test
    fun `formatCardNumber inserts spaces at Visa gaps`() {
        val result = CardNumberFormatter.formatCardNumber("4111111111111111", intArrayOf(4, 8, 12))
        assertEquals("4111 1111 1111 1111", result)
    }

    @Test
    fun `formatCardNumber inserts spaces at Amex gaps`() {
        val result = CardNumberFormatter.formatCardNumber("378282246310005", intArrayOf(4, 10))
        assertEquals("3782 822463 10005", result)
    }

    @Test
    fun `formatCardNumber returns empty string for empty input`() {
        assertEquals("", CardNumberFormatter.formatCardNumber("", intArrayOf(4, 8, 12)))
    }

    @Test
    fun `formatCardNumber handles partial input shorter than first gap`() {
        val result = CardNumberFormatter.formatCardNumber("411", intArrayOf(4, 8, 12))
        assertEquals("411", result)
    }

    @Test
    fun `formatCardNumber handles partial input at first gap boundary`() {
        val result = CardNumberFormatter.formatCardNumber("41111", intArrayOf(4, 8, 12))
        assertEquals("4111 1", result)
    }

    @Test
    fun `formatCardNumber handles input exactly at first gap`() {
        val result = CardNumberFormatter.formatCardNumber("4111", intArrayOf(4, 8, 12))
        assertEquals("4111", result)
    }

    @Test
    fun `formatCardNumber handles Diners Club format`() {
        val result = CardNumberFormatter.formatCardNumber("36111111111111", intArrayOf(4, 10))
        assertEquals("3611 111111 1111", result)
    }

    @Test
    fun `resolveBrand returns VISA for prefix 4`() {
        assertEquals(CardBrand.VISA, CardNumberFormatter.resolveBrand("4"))
    }

    @Test
    fun `resolveBrand returns DISCOVER for prefix 622`() {
        assertEquals(CardBrand.DISCOVER, CardNumberFormatter.resolveBrand("622"))
    }

    @Test
    fun `resolveBrand returns UNKNOWN for no match`() {
        assertEquals(CardBrand.UNKNOWN, CardNumberFormatter.resolveBrand("0"))
    }

    @Test
    fun `countDigitsBeforeIndex counts digits only`() {
        assertEquals(4, CardNumberFormatter.countDigitsBeforeIndex("4111 1", 5))
    }

    @Test
    fun `findIndexForDigitPosition finds correct position in formatted text`() {
        // "4111 1111" — 5th digit is at char index 5, so cursor after it is 6
        assertEquals(6, CardNumberFormatter.findIndexForDigitPosition("4111 1111", 5))
    }

    @Test
    fun `findIndexForDigitPosition returns text length when position exceeds digits`() {
        assertEquals(4, CardNumberFormatter.findIndexForDigitPosition("4111", 10))
    }

    @Test
    fun `findIndexForDigitPosition for zero digits returns zero`() {
        assertEquals(0, CardNumberFormatter.findIndexForDigitPosition("4111 1111", 0))
    }

    @Test
    fun `findIndexForDigitPosition at gap boundary`() {
        // "4111 1111" — 4th digit is at char index 3, cursor after = 4
        assertEquals(4, CardNumberFormatter.findIndexForDigitPosition("4111 1111", 4))
    }

    @Test
    fun `onBrandChanged is called when brand changes`() {
        val brands = mutableListOf<CardBrand>()
        val formatter = CardNumberFormatter { brands.add(it) }

        formatter.afterTextChanged(SpannableStringBuilder("4"))

        assertEquals(listOf(CardBrand.VISA), brands)
    }

    @Test
    fun `onBrandChanged is not called when brand stays the same`() {
        val brands = mutableListOf<CardBrand>()
        val formatter = CardNumberFormatter { brands.add(it) }

        formatter.afterTextChanged(SpannableStringBuilder("4"))
        formatter.afterTextChanged(SpannableStringBuilder("41"))

        assertEquals(listOf(CardBrand.VISA), brands)
    }

    @Test
    fun `onBrandChanged is called again when brand changes back to UNKNOWN`() {
        val brands = mutableListOf<CardBrand>()
        val formatter = CardNumberFormatter { brands.add(it) }

        formatter.afterTextChanged(SpannableStringBuilder("4"))
        formatter.afterTextChanged(SpannableStringBuilder(""))

        assertEquals(listOf(CardBrand.VISA, CardBrand.UNKNOWN), brands)
    }

    @Test
    fun `afterTextChanged formats Amex number with spaces`() {
        val formatter = CardNumberFormatter {}
        val editable = SpannableStringBuilder("378282246310005")
        formatter.afterTextChanged(editable)

        assertEquals("3782 822463 10005", editable.toString())
    }

    @Test
    fun `afterTextChanged truncates digits beyond max length for Visa`() {
        val formatter = CardNumberFormatter {}
        val editable = SpannableStringBuilder("41111111111111119999")
        formatter.afterTextChanged(editable)

        assertEquals("4111 1111 1111 1111", editable.toString())
    }

    @Test
    fun `afterTextChanged handles empty text`() {
        val formatter = CardNumberFormatter {}
        val editable = SpannableStringBuilder("")
        formatter.afterTextChanged(editable)

        assertEquals("", editable.toString())
    }

    @Test
    fun `afterTextChanged does not reformat if text is already formatted`() {
        val formatter = CardNumberFormatter {}
        val editable = SpannableStringBuilder("4111 1")
        formatter.afterTextChanged(editable)

        assertEquals("4111 1", editable.toString())
    }

    @Test
    fun `currentBrand updates after afterTextChanged`() {
        val formatter = CardNumberFormatter {}

        formatter.afterTextChanged(SpannableStringBuilder("37"))
        assertEquals(CardBrand.AMEX, formatter.currentBrand)

        formatter.afterTextChanged(SpannableStringBuilder("4"))
        assertEquals(CardBrand.VISA, formatter.currentBrand)
    }
}
