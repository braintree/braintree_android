package com.braintreepayments.api.uicomponents

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FiSummaryUnitTest {

    // Sanity: R drawable ids must be distinct here, otherwise the assertions below are meaningless.
    @Test
    fun `drawable ids are distinct`() {
        assertNotEquals(R.drawable.edit_fi_generic_bank, R.drawable.edit_fi_generic_card)
        assertNotEquals(R.drawable.paypal_monogram, R.drawable.card_fields_cc_visa)
    }

    // region iconRes - type resolution

    @Test
    fun `iconRes returns generic bank glyph for BANK`() {
        assertEquals(
            R.drawable.edit_fi_generic_bank,
            FiSummary(last4 = "3339", type = FiType.BANK).iconRes
        )
    }

    @Test
    fun `iconRes returns null for PAY_LATER`() {
        assertNull(FiSummary(type = FiType.PAY_LATER, displayName = "Pay in 4").iconRes)
    }

    @Test
    fun `iconRes returns PayPal monogram for PAYPAL`() {
        assertEquals(
            R.drawable.paypal_monogram,
            FiSummary(type = FiType.PAYPAL, displayName = "PayPal Credit").iconRes
        )
    }

    // endregion

    // region iconRes - card brand art

    @Test
    fun `iconRes returns Visa art for Visa card`() {
        assertEquals(
            R.drawable.card_fields_cc_visa,
            FiSummary(brand = "Visa", last4 = "3339", type = FiType.CARD).iconRes
        )
    }

    @Test
    fun `iconRes returns Mastercard art for Mastercard card`() {
        assertEquals(
            R.drawable.card_fields_cc_mastercard,
            FiSummary(brand = "Mastercard", type = FiType.CARD).iconRes
        )
    }

    // endregion

    // region iconRes - generic card fallback

    @Test
    fun `iconRes returns generic card glyph for null brand`() {
        assertEquals(
            R.drawable.edit_fi_generic_card,
            FiSummary(brand = null, last4 = "4242", type = FiType.CARD).iconRes
        )
    }

    @Test
    fun `iconRes returns generic card glyph for brand without dedicated art`() {
        // Diners Club resolves to a CardBrand but has no dedicated art → generic fallback glyph.
        assertEquals(
            R.drawable.edit_fi_generic_card,
            FiSummary(brand = "Diners Club", type = FiType.CARD).iconRes
        )
    }

    // endregion
}
