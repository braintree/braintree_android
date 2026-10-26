package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalCreditMessagingContent
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalCreditMessagingDisplayState
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethod
import com.braintreepayments.api.paypalsavedpaymentmethod.model.PayPalSavedPaymentMethodDisplayState
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
    private val message = PayPalCreditMessagingDisplayState.Content(
        PayPalCreditMessagingContent(message = "Pay in 4", learnMoreText = null, learnMoreUrl = null)
    )

    @Test
    fun `iconRes returns the card and bank icons`() {
        assertEquals(R.drawable.ic_fi_card_placeholder, PayPalSavedPaymentMethodType.CARD.iconRes())
        assertEquals(R.drawable.ic_fi_bank_placeholder, PayPalSavedPaymentMethodType.BANK.iconRes())
    }

    @Test
    fun `iconRes returns null for PayPal Credit and unknown types`() {
        assertNull(PayPalSavedPaymentMethodType.PAYPAL_CREDIT.iconRes())
        assertNull((null as PayPalSavedPaymentMethodType?).iconRes())
    }

    @Test
    fun `maskedLastDigitsOrNull returns the last digits for a card or bank`() {
        assertEquals("1234", card.maskedLastDigitsOrNull())
        assertEquals("1234", card.copy(type = PayPalSavedPaymentMethodType.BANK).maskedLastDigitsOrNull())
    }

    @Test
    fun `maskedLastDigitsOrNull returns null for PayPal Credit placeholder digits`() {
        val payPalCredit = card.copy(type = PayPalSavedPaymentMethodType.PAYPAL_CREDIT, lastDigits = "0000")

        assertNull(payPalCredit.maskedLastDigitsOrNull())
    }

    @Test
    fun `maskedLastDigitsOrNull returns null for an unknown type`() {
        assertNull(card.copy(type = null).maskedLastDigitsOrNull())
    }

    @Test
    fun `maskedLastDigitsOrNull returns null when the last digits are missing or blank`() {
        assertNull(card.copy(lastDigits = null).maskedLastDigitsOrNull())
        assertNull(card.copy(lastDigits = " ").maskedLastDigitsOrNull())
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

        assertEquals("Visa, ending in 1 2 3 4", content.accessibilityLabel(::endingIn))
    }

    @Test
    fun `accessibilityLabel reads the label alone when there are no last digits to read`() {
        val payPalCredit = PayPalSavedPaymentMethodDisplayState.Content(
            card.copy(type = PayPalSavedPaymentMethodType.PAYPAL_CREDIT, label = "PayPal Credit", lastDigits = "0000")
        )

        assertEquals("PayPal Credit", payPalCredit.accessibilityLabel(::endingIn))
    }

    @Test
    fun `accessibilityLabel reads the email when there is no funding instrument`() {
        val emailOnly =
            PayPalSavedPaymentMethodDisplayState.NoFundingInstrument("buyer@example.com", isEditable = false)

        assertEquals("buyer@example.com", emailOnly.accessibilityLabel(::endingIn))
    }

    @Test
    fun `accessibilityLabel is empty for states without a funding instrument chip`() {
        listOf(
            PayPalSavedPaymentMethodDisplayState.Loading,
            PayPalSavedPaymentMethodDisplayState.NoNetwork,
            PayPalSavedPaymentMethodDisplayState.Error,
            PayPalSavedPaymentMethodDisplayState.Hidden
        ).forEach { state ->
            assertEquals("", state.accessibilityLabel(::endingIn))
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

    // The isEditable tests type the state as the sealed interface, so `isEditable` resolves to the shared rule rather
    // than the email state's own property.
    @Test
    fun `a funding instrument is editable`() {
        val content: PayPalSavedPaymentMethodDisplayState = PayPalSavedPaymentMethodDisplayState.Content(card)

        assertTrue(content.isEditable)
    }

    @Test
    fun `an email is editable when the buyer can edit`() {
        val emailOnly: PayPalSavedPaymentMethodDisplayState =
            PayPalSavedPaymentMethodDisplayState.NoFundingInstrument("buyer@example.com", isEditable = true)

        assertTrue(emailOnly.isEditable)
    }

    @Test
    fun `an email is not editable when the buyer can't edit`() {
        val emailOnly: PayPalSavedPaymentMethodDisplayState =
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

    @Test
    fun `the Pay Later message shows while the funding instrument loads or next to a funding instrument`() {
        val loading = PayPalSavedPaymentMethodDisplayState.Loading
        val content = PayPalSavedPaymentMethodDisplayState.Content(card)

        assertEquals(message, message.visibleWith(loading, isEditCompleted = false))
        assertEquals(message, message.visibleWith(content, isEditCompleted = false))
    }

    @Test
    fun `the Pay Later message shows next to an email only when the buyer can edit`() {
        val editable = PayPalSavedPaymentMethodDisplayState.NoFundingInstrument("buyer@example.com", isEditable = true)
        val notEditable = editable.copy(isEditable = false)

        assertEquals(message, message.visibleWith(editable, isEditCompleted = false))
        assertEquals(Hidden, message.visibleWith(notEditable, isEditCompleted = false))
    }

    @Test
    fun `the Pay Later message is hidden next to the PayPal logo alone`() {
        assertEquals(Hidden, message.visibleWith(PayPalSavedPaymentMethodDisplayState.Error, isEditCompleted = false))
        assertEquals(
            Hidden,
            message.visibleWith(PayPalSavedPaymentMethodDisplayState.NoNetwork, isEditCompleted = false)
        )
    }

    @Test
    fun `the Pay Later message is hidden when the component is hidden`() {
        assertEquals(Hidden, message.visibleWith(PayPalSavedPaymentMethodDisplayState.Hidden, isEditCompleted = false))
    }

    @Test
    fun `the Pay Later message is hidden after an edit`() {
        val content = PayPalSavedPaymentMethodDisplayState.Content(card)

        assertEquals(Hidden, message.visibleWith(content, isEditCompleted = true))
        assertEquals(Hidden, message.visibleWith(PayPalSavedPaymentMethodDisplayState.Loading, isEditCompleted = true))
    }

    @Test
    fun `the Pay Later shimmer only shows while the funding instrument loads`() {
        val loading = PayPalCreditMessagingDisplayState.Loading

        assertEquals(
            loading,
            loading.visibleWith(PayPalSavedPaymentMethodDisplayState.Loading, isEditCompleted = false)
        )
        assertEquals(
            Hidden,
            loading.visibleWith(PayPalSavedPaymentMethodDisplayState.Content(card), isEditCompleted = false)
        )
    }

    @Test
    fun `the Pay Later shimmer is hidden next to an email`() {
        val editableEmail =
            PayPalSavedPaymentMethodDisplayState.NoFundingInstrument("buyer@example.com", isEditable = true)

        val loading = PayPalCreditMessagingDisplayState.Loading

        assertEquals(Hidden, loading.visibleWith(editableEmail, isEditCompleted = false))
    }

    @Test
    fun `the Pay Later shimmer is hidden after an edit`() {
        val loading = PayPalCreditMessagingDisplayState.Loading

        assertEquals(Hidden, loading.visibleWith(PayPalSavedPaymentMethodDisplayState.Loading, isEditCompleted = true))
    }

    @Test
    fun `a hidden Pay Later message stays hidden next to a funding instrument`() {
        val content = PayPalSavedPaymentMethodDisplayState.Content(card)

        assertEquals(Hidden, Hidden.visibleWith(content, isEditCompleted = false))
        assertEquals(Hidden, Hidden.visibleWith(PayPalSavedPaymentMethodDisplayState.Loading, isEditCompleted = false))
    }

    private fun endingIn(label: String, spokenDigits: String) = "$label, ending in $spokenDigits"

    private companion object {
        val Hidden = PayPalCreditMessagingDisplayState.Hidden
    }
}
