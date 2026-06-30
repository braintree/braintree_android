package com.braintreepayments.api.uicomponents.cardfields

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CardBrandUnitTest {

    // region Brand detection - single brand matches

    @Test
    fun `detect returns VISA for prefix 4`() {
        assertEquals(listOf(CardBrand.VISA), CardBrand.detect("4"))
    }

    @Test
    fun `detect returns VISA for full card number`() {
        assertEquals(listOf(CardBrand.VISA), CardBrand.detect("4111111111111111"))
    }

    @Test
    fun `detect returns MASTERCARD for prefix 51`() {
        assertEquals(listOf(CardBrand.MASTERCARD), CardBrand.detect("51"))
    }

    @Test
    fun `detect returns MASTERCARD for prefix 55`() {
        assertEquals(listOf(CardBrand.MASTERCARD), CardBrand.detect("55"))
    }

    @Test
    fun `detect returns MASTERCARD for prefix 2221`() {
        assertEquals(listOf(CardBrand.MASTERCARD), CardBrand.detect("2221"))
    }

    @Test
    fun `detect returns MASTERCARD for prefix 2720`() {
        assertEquals(listOf(CardBrand.MASTERCARD), CardBrand.detect("2720"))
    }

    @Test
    fun `detect returns AMEX for prefix 34`() {
        assertEquals(listOf(CardBrand.AMEX), CardBrand.detect("34"))
    }

    @Test
    fun `detect returns AMEX for prefix 37`() {
        assertEquals(listOf(CardBrand.AMEX), CardBrand.detect("37"))
    }

    @Test
    fun `detect returns DISCOVER for prefix 6011`() {
        assertEquals(listOf(CardBrand.DISCOVER), CardBrand.detect("6011"))
    }

    @Test
    fun `detect returns DISCOVER for prefix 65`() {
        val result = CardBrand.detect("65")
        assertTrue(result.contains(CardBrand.DISCOVER))
    }

    @Test
    fun `detect returns DISCOVER for prefix 644`() {
        assertEquals(listOf(CardBrand.DISCOVER), CardBrand.detect("644"))
    }

    @Test
    fun `detect returns JCB for prefix 3528`() {
        assertEquals(listOf(CardBrand.JCB), CardBrand.detect("3528"))
    }

    @Test
    fun `detect returns DINERS_CLUB for prefix 36`() {
        assertEquals(listOf(CardBrand.DINERS_CLUB), CardBrand.detect("36"))
    }

    @Test
    fun `detect returns DINERS_CLUB for prefix 305`() {
        assertEquals(listOf(CardBrand.DINERS_CLUB), CardBrand.detect("305"))
    }

    @Test
    fun `detect returns DINERS_CLUB for prefix 38`() {
        assertEquals(listOf(CardBrand.DINERS_CLUB), CardBrand.detect("38"))
    }

    @Test
    fun `detect returns MAESTRO for prefix 5018`() {
        assertEquals(listOf(CardBrand.MAESTRO), CardBrand.detect("5018"))
    }

    @Test
    fun `detect returns MAESTRO for prefix 6759`() {
        assertEquals(listOf(CardBrand.MAESTRO), CardBrand.detect("6759"))
    }

    @Test
    fun `detect returns MAESTRO for prefix 6761`() {
        assertEquals(listOf(CardBrand.MAESTRO), CardBrand.detect("6761"))
    }

    @Test
    fun `detect returns UNIONPAY for prefix 621`() {
        assertEquals(listOf(CardBrand.UNIONPAY), CardBrand.detect("621"))
    }

    @Test
    fun `detect returns HIPER for prefix 637095`() {
        assertEquals(listOf(CardBrand.HIPER), CardBrand.detect("637095"))
    }

    @Test
    fun `detect returns HIPER for prefix 637568`() {
        assertEquals(listOf(CardBrand.HIPER), CardBrand.detect("637568"))
    }

    @Test
    fun `detect returns HIPERCARD for prefix 606282`() {
        assertEquals(listOf(CardBrand.HIPERCARD), CardBrand.detect("606282"))
    }

    // region Brand detection - ELO

    @Test
    fun `detect returns ELO for prefix 401178`() {
        val result = CardBrand.detect("401178")
        assertTrue(result.contains(CardBrand.ELO))
    }

    @Test
    fun `detect returns ELO for prefix 509123`() {
        assertEquals(listOf(CardBrand.ELO), CardBrand.detect("509123"))
    }

    // endregion

    // region Brand detection - MIR

    @Test
    fun `detect returns MIR for prefix 2200`() {
        assertEquals(listOf(CardBrand.MIR), CardBrand.detect("2200"))
    }

    @Test
    fun `detect does not return MIR for prefix 2205`() {
        val result = CardBrand.detect("2205")
        assertTrue(!result.contains(CardBrand.MIR))
    }

    // endregion

    // region Brand detection - VERVE

    @Test
    fun `detect returns VERVE for prefix 506099`() {
        val result = CardBrand.detect("506099")
        assertTrue(result.contains(CardBrand.VERVE))
    }

    @Test
    fun `detect returns VERVE for prefix 507865`() {
        assertEquals(listOf(CardBrand.VERVE), CardBrand.detect("507865"))
    }

    // endregion

    // region Brand detection - ELO and UNIONPAY overlap

    @Test
    fun `detect returns ELO and UNIONPAY for prefix 627780`() {
        val result = CardBrand.detect("627780")
        assertTrue(result.contains(CardBrand.ELO))
        assertTrue(result.contains(CardBrand.UNIONPAY))
    }

    // endregion

    // region Brand detection - ambiguous prefixes

    @Test
    fun `detect returns only DISCOVER for prefix 622`() {
        val result = CardBrand.detect("622")
        assertEquals(listOf(CardBrand.DISCOVER), result)
    }

    @Test
    fun `detect returns only JCB for prefix 35`() {
        val result = CardBrand.detect("35")
        assertEquals(listOf(CardBrand.JCB), result)
    }

    // endregion

    // region Brand detection - relaxed matching

    @Test
    fun `detect falls back to relaxed patterns when no strict match`() {
        val result = CardBrand.detect("60")
        assertEquals(listOf(CardBrand.MAESTRO), result)
    }

    @Test
    fun `detect prefers strict match over relaxed match`() {
        val result = CardBrand.detect("62")
        assertEquals(listOf(CardBrand.UNIONPAY), result)
    }

    // endregion

    // region Brand detection - no match

    @Test
    fun `detect returns empty list for prefix 0`() {
        assertTrue(CardBrand.detect("0").isEmpty())
    }

    @Test
    fun `detect returns empty list for prefix 9`() {
        assertTrue(CardBrand.detect("9").isEmpty())
    }

    @Test
    fun `detect returns empty list for prefix 123`() {
        assertTrue(CardBrand.detect("123").isEmpty())
    }

    @Test
    fun `detect returns empty list for single digit with no match`() {
        assertTrue(CardBrand.detect("1").isEmpty())
    }

    // endregion

    // region Brand detection - empty and special input

    @Test
    fun `detect returns empty list for empty string`() {
        assertTrue(CardBrand.detect("").isEmpty())
    }

    @Test
    fun `detect strips non-digit characters before matching`() {
        assertEquals(listOf(CardBrand.VISA), CardBrand.detect("4 111"))
    }

    @Test
    fun `detect returns empty list for non-digit-only input`() {
        assertTrue(CardBrand.detect("abc").isEmpty())
    }

    // endregion

    // region Brand detection - UNKNOWN never returned

    @Test
    fun `detect never returns UNKNOWN`() {
        val inputs = listOf("", "0", "4", "34", "51", "6011", "9999999999")
        inputs.forEach { input ->
            val result = CardBrand.detect(input)
            assertTrue(
                "UNKNOWN should not be in results for input '$input'",
                !result.contains(CardBrand.UNKNOWN)
            )
        }
    }

    // endregion

    // region Property verification

    @Test
    fun `VISA has correct properties`() {
        assertEquals(setOf(16), CardBrand.VISA.validLengths)
        assertEquals(16, CardBrand.VISA.minLength)
        assertEquals(16, CardBrand.VISA.maxLength)
        assertEquals(3, CardBrand.VISA.cvvLength)
        assertTrue(intArrayOf(4, 8, 12).contentEquals(CardBrand.VISA.formatGaps))
    }

    @Test
    fun `AMEX has correct properties`() {
        assertEquals(setOf(15), CardBrand.AMEX.validLengths)
        assertEquals(4, CardBrand.AMEX.cvvLength)
        assertTrue(intArrayOf(4, 10).contentEquals(CardBrand.AMEX.formatGaps))
    }

    @Test
    fun `DINERS_CLUB has correct properties`() {
        assertEquals(setOf(14), CardBrand.DINERS_CLUB.validLengths)
        assertEquals(3, CardBrand.DINERS_CLUB.cvvLength)
        assertTrue(intArrayOf(4, 10).contentEquals(CardBrand.DINERS_CLUB.formatGaps))
    }

    @Test
    fun `DISCOVER has variable length`() {
        assertEquals(setOf(16, 17, 18, 19), CardBrand.DISCOVER.validLengths)
        assertEquals(16, CardBrand.DISCOVER.minLength)
        assertEquals(19, CardBrand.DISCOVER.maxLength)
    }

    @Test
    fun `MAESTRO has variable length`() {
        assertEquals(setOf(12, 13, 14, 15, 16, 17, 18, 19), CardBrand.MAESTRO.validLengths)
        assertEquals(12, CardBrand.MAESTRO.minLength)
        assertEquals(19, CardBrand.MAESTRO.maxLength)
    }

    @Test
    fun `UNKNOWN has empty prefix patterns`() {
        assertTrue(CardBrand.UNKNOWN.prefixPatterns.isEmpty())
    }

    @Test
    fun `all brands have minLength less than or equal to maxLength`() {
        CardBrand.entries.forEach { brand ->
            assertTrue(
                "${brand.name} minLength (${brand.minLength}) should be <= maxLength (${brand.maxLength})",
                brand.minLength <= brand.maxLength
            )
        }
    }

    // endregion
}
