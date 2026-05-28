package com.braintreepayments.api.uicomponents.cardfields

import com.braintreepayments.api.uicomponents.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardFieldsViewModelUnitTest {

    private val fixedDate = Pair(6, 2026)

    private fun createViewModel() = CardFieldsViewModel(
        currentDateProvider = { fixedDate }
    )

    // region Card Number Validation

    @Test
    fun `valid Visa updates detected brand to VISA`() {
        val sut = createViewModel()
        sut.onCardNumberChanged("4111111111111111")
        assertEquals(CardBrand.VISA, sut.detectedCardBrand.value)
    }

    @Test
    fun `valid Amex updates detected brand to AMEX`() {
        val sut = createViewModel()
        sut.onCardNumberChanged("378282246310005")
        assertEquals(CardBrand.AMEX, sut.detectedCardBrand.value)
    }

    @Test
    fun `empty card number sets brand to UNKNOWN`() {
        val sut = createViewModel()
        sut.onCardNumberChanged("4111111111111111")
        sut.onCardNumberChanged("")
        assertEquals(CardBrand.UNKNOWN, sut.detectedCardBrand.value)
    }

    @Test
    fun `valid card number does not show error after focus out`() {
        val sut = createViewModel()
        sut.onCardNumberChanged("4111111111111111")
        sut.onFieldFocusChanged(CardField.CARD_NUMBER, hasFocus = false)
        assertNull(sut.cardNumberError.value)
    }

    @Test
    fun `invalid card number shows error after focus out`() {
        val sut = createViewModel()
        sut.onCardNumberChanged("4111111111111112")
        sut.onFieldFocusChanged(CardField.CARD_NUMBER, hasFocus = false)
        assertEquals(R.string.card_number_error, sut.cardNumberError.value)
    }

    @Test
    fun `invalid card number does not show error before focus out`() {
        val sut = createViewModel()
        sut.onCardNumberChanged("4111111111111112")
        assertNull(sut.cardNumberError.value)
    }

    @Test
    fun `card number error clears when corrected`() {
        val sut = createViewModel()
        sut.onCardNumberChanged("4111111111111112")
        sut.onFieldFocusChanged(CardField.CARD_NUMBER, hasFocus = false)
        assertEquals(R.string.card_number_error, sut.cardNumberError.value)

        sut.onCardNumberChanged("4111111111111111")
        assertNull(sut.cardNumberError.value)
    }

    @Test
    fun `empty card number does not show error after focus out`() {
        val sut = createViewModel()
        sut.onCardNumberChanged("")
        sut.onFieldFocusChanged(CardField.CARD_NUMBER, hasFocus = false)
        assertNull(sut.cardNumberError.value)
    }

    // endregion

    // region Expiry Validation

    @Test
    fun `valid future expiry does not show error after focus out`() {
        val sut = createViewModel()
        sut.onExpiryChanged("1227")
        sut.onFieldFocusChanged(CardField.EXPIRY, hasFocus = false)
        assertNull(sut.expiryError.value)
    }

    @Test
    fun `expired date shows error after focus out`() {
        val sut = createViewModel()
        sut.onExpiryChanged("0125")
        sut.onFieldFocusChanged(CardField.EXPIRY, hasFocus = false)
        assertEquals(R.string.expiration_error, sut.expiryError.value)
    }

    @Test
    fun `invalid expiry does not show error before focus out`() {
        val sut = createViewModel()
        sut.onExpiryChanged("0125")
        assertNull(sut.expiryError.value)
    }

    @Test
    fun `expiry error clears when corrected`() {
        val sut = createViewModel()
        sut.onExpiryChanged("0125")
        sut.onFieldFocusChanged(CardField.EXPIRY, hasFocus = false)
        assertEquals(R.string.expiration_error, sut.expiryError.value)

        sut.onExpiryChanged("1227")
        assertNull(sut.expiryError.value)
    }

    @Test
    fun `current month and year is valid`() {
        val sut = createViewModel()
        sut.onExpiryChanged("0626")
        sut.onFieldFocusChanged(CardField.EXPIRY, hasFocus = false)
        assertNull(sut.expiryError.value)
    }

    @Test
    fun `incomplete expiry shows error after focus out`() {
        val sut = createViewModel()
        sut.onExpiryChanged("12")
        sut.onFieldFocusChanged(CardField.EXPIRY, hasFocus = false)
        assertEquals(R.string.expiration_error, sut.expiryError.value)
    }

    // endregion

    // region CVV Validation

    @Test
    fun `valid 3-digit CVV does not show error after focus out`() {
        val sut = createViewModel()
        sut.onCvvChanged("123")
        sut.onFieldFocusChanged(CardField.CVV, hasFocus = false)
        assertNull(sut.cvvError.value)
    }

    @Test
    fun `too short CVV shows error after focus out`() {
        val sut = createViewModel()
        sut.onCvvChanged("12")
        sut.onFieldFocusChanged(CardField.CVV, hasFocus = false)
        assertEquals(R.string.cvv_error, sut.cvvError.value)
    }

    @Test
    fun `invalid CVV does not show error before focus out`() {
        val sut = createViewModel()
        sut.onCvvChanged("12")
        assertNull(sut.cvvError.value)
    }

    @Test
    fun `CVV error clears when corrected`() {
        val sut = createViewModel()
        sut.onCvvChanged("12")
        sut.onFieldFocusChanged(CardField.CVV, hasFocus = false)
        assertEquals(R.string.cvv_error, sut.cvvError.value)

        sut.onCvvChanged("123")
        assertNull(sut.cvvError.value)
    }

    @Test
    fun `empty CVV does not show error after focus out`() {
        val sut = createViewModel()
        sut.onCvvChanged("")
        sut.onFieldFocusChanged(CardField.CVV, hasFocus = false)
        assertNull(sut.cvvError.value)
    }

    @Test
    fun `4-digit CVV valid for Amex`() {
        val sut = createViewModel()
        sut.onCardNumberChanged("378282246310005")
        sut.onCvvChanged("1234")
        sut.onFieldFocusChanged(CardField.CVV, hasFocus = false)
        assertNull(sut.cvvError.value)
    }

    @Test
    fun `3-digit CVV invalid for Amex`() {
        val sut = createViewModel()
        sut.onCardNumberChanged("378282246310005")
        sut.onCvvChanged("123")
        sut.onFieldFocusChanged(CardField.CVV, hasFocus = false)
        assertEquals(R.string.cvv_error, sut.cvvError.value)
    }

    // endregion

    // region CVV Re-validation on Brand Change

    @Test
    fun `CVV re-validated when card brand changes`() {
        val sut = createViewModel()
        sut.onCvvChanged("123")
        sut.onFieldFocusChanged(CardField.CVV, hasFocus = false)
        assertNull(sut.cvvError.value)

        sut.onCardNumberChanged("378282246310005")
        assertEquals(R.string.cvv_error, sut.cvvError.value)
    }

    @Test
    fun `CVV error clears when brand changes back to 3-digit`() {
        val sut = createViewModel()
        sut.onCvvChanged("123")
        sut.onFieldFocusChanged(CardField.CVV, hasFocus = false)
        sut.onCardNumberChanged("378282246310005")
        assertEquals(R.string.cvv_error, sut.cvvError.value)

        sut.onCardNumberChanged("4111111111111111")
        assertNull(sut.cvvError.value)
    }

    // endregion

    // region isValid

    @Test
    fun `isValid false initially`() {
        val sut = createViewModel()
        assertFalse(sut.isValid.value)
    }

    @Test
    fun `isValid true when all fields are valid`() {
        val sut = createViewModel()
        sut.onCardNumberChanged("4111111111111111")
        sut.onExpiryChanged("1227")
        sut.onCvvChanged("123")
        assertTrue(sut.isValid.value)
    }

    @Test
    fun `isValid false when card number is invalid`() {
        val sut = createViewModel()
        sut.onCardNumberChanged("4111111111111112")
        sut.onExpiryChanged("1227")
        sut.onCvvChanged("123")
        assertFalse(sut.isValid.value)
    }

    @Test
    fun `isValid false when expiry is invalid`() {
        val sut = createViewModel()
        sut.onCardNumberChanged("4111111111111111")
        sut.onExpiryChanged("0125")
        sut.onCvvChanged("123")
        assertFalse(sut.isValid.value)
    }

    @Test
    fun `isValid false when CVV is invalid`() {
        val sut = createViewModel()
        sut.onCardNumberChanged("4111111111111111")
        sut.onExpiryChanged("1227")
        sut.onCvvChanged("12")
        assertFalse(sut.isValid.value)
    }

    @Test
    fun `isValid updates from false to true when last field becomes valid`() {
        val sut = createViewModel()
        sut.onCardNumberChanged("4111111111111111")
        sut.onExpiryChanged("1227")
        assertFalse(sut.isValid.value)

        sut.onCvvChanged("123")
        assertTrue(sut.isValid.value)
    }

    @Test
    fun `isValid updates from true to false when a field becomes invalid`() {
        val sut = createViewModel()
        sut.onCardNumberChanged("4111111111111111")
        sut.onExpiryChanged("1227")
        sut.onCvvChanged("123")
        assertTrue(sut.isValid.value)

        sut.onCardNumberChanged("411111")
        assertFalse(sut.isValid.value)
    }

    // endregion

    // region Focus Gating

    @Test
    fun `focus gained does not trigger error display`() {
        val sut = createViewModel()
        sut.onCardNumberChanged("4111111111111112")
        sut.onFieldFocusChanged(CardField.CARD_NUMBER, hasFocus = true)
        assertNull(sut.cardNumberError.value)
    }

    @Test
    fun `subsequent text changes show errors after field was touched`() {
        val sut = createViewModel()
        sut.onCardNumberChanged("4111111111111112")
        sut.onFieldFocusChanged(CardField.CARD_NUMBER, hasFocus = false)
        assertEquals(R.string.card_number_error, sut.cardNumberError.value)

        sut.onCardNumberChanged("411111")
        assertEquals(R.string.card_number_error, sut.cardNumberError.value)
    }

    // endregion

    // region Default State

    @Test
    fun `initial state has no errors`() {
        val sut = createViewModel()
        assertNull(sut.cardNumberError.value)
        assertNull(sut.expiryError.value)
        assertNull(sut.cvvError.value)
    }

    @Test
    fun `initial detected brand is UNKNOWN`() {
        val sut = createViewModel()
        assertEquals(CardBrand.UNKNOWN, sut.detectedCardBrand.value)
    }

    // endregion
}
