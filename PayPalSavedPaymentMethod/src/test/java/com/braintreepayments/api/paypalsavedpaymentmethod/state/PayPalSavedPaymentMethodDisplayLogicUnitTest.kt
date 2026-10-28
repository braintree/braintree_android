package com.braintreepayments.api.paypalsavedpaymentmethod.state

import com.braintreepayments.api.paypalsavedpaymentmethod.R
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PayPalSavedPaymentMethodDisplayLogicUnitTest {

    private val card = PayPalSavedPaymentMethod(
        type = PayPalSavedPaymentMethodType.CARD,
        label = "Visa",
        imageUrl = null,
        lastDigits = "1234",
        subtype = null
    )

    @Test
    fun `iconRes returns the card and bank icons`() {
        assertEquals(R.drawable.ic_fi_card_placeholder, card.iconRes())
        assertEquals(R.drawable.ic_fi_bank_placeholder, card.copy(type = PayPalSavedPaymentMethodType.BANK).iconRes())
    }

    @Test
    fun `iconRes returns null for PayPal Credit and unknown types`() {
        assertNull(card.copy(type = PayPalSavedPaymentMethodType.PAYPAL_CREDIT).iconRes())
        assertNull(card.copy(type = null).iconRes())
    }

    @Test
    fun `a card or bank with a subtype shows its label, without digits or an icon`() {
        listOf(PayPalSavedPaymentMethodType.CARD, PayPalSavedPaymentMethodType.BANK).forEach { type ->
            val payPalProduct = card.copy(type = type, label = "PayPal Debit Card", subtype = "PAYPAL_DEBIT")

            assertNull(payPalProduct.lastDigitsOrNull())
            assertNull(payPalProduct.spokenLastDigitsOrNull())
            assertNull(payPalProduct.iconRes())
            assertEquals(
                "PayPal Debit Card",
                PayPalSavedPaymentMethodDisplayState.Content(payPalProduct).accessibilityLabel(null, ::endingIn)
            )
        }
    }

    @Test
    fun `a card without last digits has no icon`() {
        assertNull(card.copy(lastDigits = null).iconRes())
        assertNull(card.copy(lastDigits = " ").iconRes())
    }

    @Test
    fun `lastDigitsOrNull returns the last digits for a card or bank`() {
        assertEquals("1234", card.lastDigitsOrNull())
        assertEquals("1234", card.copy(type = PayPalSavedPaymentMethodType.BANK).lastDigitsOrNull())
    }

    @Test
    fun `lastDigitsOrNull returns null for PayPal Credit placeholder digits`() {
        val payPalCredit = card.copy(type = PayPalSavedPaymentMethodType.PAYPAL_CREDIT, lastDigits = "0000")

        assertNull(payPalCredit.lastDigitsOrNull())
    }

    @Test
    fun `lastDigitsOrNull returns null for an unknown type`() {
        assertNull(card.copy(type = null).lastDigitsOrNull())
    }

    @Test
    fun `lastDigitsOrNull returns null when the last digits are missing or blank`() {
        assertNull(card.copy(lastDigits = null).lastDigitsOrNull())
        assertNull(card.copy(lastDigits = " ").lastDigitsOrNull())
    }

    @Test
    fun `spokenLastDigitsOrNull separates the digits so each is read on its own`() {
        assertEquals("1 2 3 4", card.spokenLastDigitsOrNull())
        assertEquals("1 2 3 4", card.copy(type = PayPalSavedPaymentMethodType.BANK).spokenLastDigitsOrNull())
    }

    @Test
    fun `spokenLastDigitsOrNull returns null when the label is read on its own`() {
        val payPalCredit = card.copy(type = PayPalSavedPaymentMethodType.PAYPAL_CREDIT, lastDigits = "0000")

        assertNull(payPalCredit.spokenLastDigitsOrNull())
        assertNull(card.copy(lastDigits = null).spokenLastDigitsOrNull())
    }

    @Test
    fun `accessibilityLabel reads the label and spaced last digits for a card`() {
        val content = PayPalSavedPaymentMethodDisplayState.Content(card)

        assertEquals("Visa, ending in 1 2 3 4", content.accessibilityLabel(null, ::endingIn))
    }

    @Test
    fun `accessibilityLabel reads the label alone when there are no last digits to read`() {
        val payPalCredit = PayPalSavedPaymentMethodDisplayState.Content(
            card.copy(type = PayPalSavedPaymentMethodType.PAYPAL_CREDIT, label = "PayPal Credit", lastDigits = "0000")
        )

        assertEquals("PayPal Credit", payPalCredit.accessibilityLabel(null, ::endingIn))
    }

    @Test
    fun `accessibilityLabel introduces a funding instrument with the brand name when the brand is hidden`() {
        val content = PayPalSavedPaymentMethodDisplayState.Content(card)
        val emailOnly = PayPalSavedPaymentMethodDisplayState.NoFundingInstrument("buyer@example.com", isEditable = true)

        assertEquals("PayPal, Visa, ending in 1 2 3 4", content.accessibilityLabel("PayPal", ::endingIn))
        assertEquals("buyer@example.com", emailOnly.accessibilityLabel("PayPal", ::endingIn))
    }

    @Test
    fun `accessibilityLabel reads the email when there is no funding instrument`() {
        val emailOnly =
            PayPalSavedPaymentMethodDisplayState.NoFundingInstrument("buyer@example.com", isEditable = false)

        assertEquals("buyer@example.com", emailOnly.accessibilityLabel(null, ::endingIn))
    }

    @Test
    fun `accessibilityLabel is null for states without a funding instrument chip`() {
        listOf(
            PayPalSavedPaymentMethodDisplayState.Loading,
            PayPalSavedPaymentMethodDisplayState.NoNetwork,
            PayPalSavedPaymentMethodDisplayState.Error,
            PayPalSavedPaymentMethodDisplayState.Hidden
        ).forEach { state ->
            assertNull(state.accessibilityLabel(null, ::endingIn))
        }
    }

    @Test
    fun `the funding instrument row shows while loading, with content, or with an email`() {
        assertTrue(PayPalSavedPaymentMethodDisplayState.Loading.showsFundingInstrumentRow)
        assertTrue(PayPalSavedPaymentMethodDisplayState.Content(card).showsFundingInstrumentRow)
        val emailOnly = PayPalSavedPaymentMethodDisplayState.NoFundingInstrument("buyer@example.com", isEditable = true)
        assertTrue(emailOnly.showsFundingInstrumentRow)
    }

    @Test
    fun `the funding instrument row is hidden for errors and no network`() {
        assertFalse(PayPalSavedPaymentMethodDisplayState.Error.showsFundingInstrumentRow)
        assertFalse(PayPalSavedPaymentMethodDisplayState.NoNetwork.showsFundingInstrumentRow)
    }

    @Test
    fun `the funding instrument row is hidden when the component is hidden`() {
        assertFalse(PayPalSavedPaymentMethodDisplayState.Hidden.showsFundingInstrumentRow)
    }

    @Test
    fun `a funding instrument is editable`() {
        val content = PayPalSavedPaymentMethodDisplayState.Content(card)

        assertTrue(content.isEditable)
    }

    @Test
    fun `an email is editable when the buyer can edit`() {
        val emailOnly = PayPalSavedPaymentMethodDisplayState.NoFundingInstrument("buyer@example.com", isEditable = true)

        assertTrue(emailOnly.isEditable)
    }

    @Test
    fun `an email is not editable when the buyer can't edit`() {
        val emailOnly =
            PayPalSavedPaymentMethodDisplayState.NoFundingInstrument("buyer@example.com", isEditable = false)

        assertFalse(emailOnly.isEditable)
    }

    @Test
    fun `nothing is editable while loading, offline, failed or hidden`() {
        listOf(
            PayPalSavedPaymentMethodDisplayState.Loading,
            PayPalSavedPaymentMethodDisplayState.NoNetwork,
            PayPalSavedPaymentMethodDisplayState.Error,
            PayPalSavedPaymentMethodDisplayState.Hidden
        ).forEach { state ->
            assertFalse(state.isEditable)
        }
    }

    private fun endingIn(label: String, spokenDigits: String) = "$label, ending in $spokenDigits"
}
