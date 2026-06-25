package com.braintreepayments.api.uicomponents.cardfields

import com.braintreepayments.api.uicomponents.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardFieldsViewModelUnitTest {

    private fun createViewModel() = CardFieldsViewModel(
        currentDateProvider = { Pair(1, 2020) }
    )

    // region Initial State

    @Test
    fun `card number validation sets to Validating on init`() {
        val vm = createViewModel()
        assertEquals(ValidationResult.Validating, vm.cardNumberValidation.value)
    }

    @Test
    fun `expiration validation sets to Validating on init`() {
        val vm = createViewModel()
        assertEquals(ValidationResult.Validating, vm.expirationValidation.value)
    }

    @Test
    fun `cvv validation sets to Validating on init`() {
        val vm = createViewModel()
        assertEquals(ValidationResult.Validating, vm.cvvValidation.value)
    }

    @Test
    fun `detected card brand sets to UNKNOWN on init`() {
        val vm = createViewModel()
        assertEquals(CardBrand.UNKNOWN, vm.detectedCardBrand.value)
    }

    @Test
    fun `isFormValid sets to false on init`() {
        val vm = createViewModel()
        assertFalse(vm.isFormValid.value)
    }

    // endregion

    // region Card Number — typing state machine

    @Test
    fun `valid card number sets Valid during typing`() {
        val vm = createViewModel()
        vm.onCardNumberChanged("4111111111111111")
        assertEquals(ValidationResult.Valid, vm.cardNumberValidation.value)
    }

    @Test
    fun `partial card number stays Validating during typing`() {
        val vm = createViewModel()
        vm.onCardNumberChanged("4111")
        assertEquals(ValidationResult.Validating, vm.cardNumberValidation.value)
    }

    @Test
    fun `invalid card number does not show error during typing`() {
        val vm = createViewModel()
        vm.onCardNumberChanged("4111111111111112")
        assertEquals(ValidationResult.Validating, vm.cardNumberValidation.value)
    }

    @Test
    fun `valid then invalid card number downgrades to Validating`() {
        val vm = createViewModel()
        vm.onCardNumberChanged("4111111111111111")
        assertEquals(ValidationResult.Valid, vm.cardNumberValidation.value)

        vm.onCardNumberChanged("41111111111111112")
        assertEquals(ValidationResult.Validating, vm.cardNumberValidation.value)
    }

    @Test
    fun `card number detects brand during typing`() {
        val vm = createViewModel()
        vm.onCardNumberChanged("4111")
        assertEquals(CardBrand.VISA, vm.detectedCardBrand.value)
    }

    // endregion

    // region Card Number — blur

    @Test
    fun `empty card number shows required error on blur`() {
        val vm = createViewModel()
        vm.onCardNumberChanged("")
        vm.onFieldFocusChanged(CardField.CARD_NUMBER, hasFocus = false)
        assertEquals(
            ValidationResult.Invalid(R.string.card_number_required),
            vm.cardNumberValidation.value
        )
    }

    @Test
    fun `partial card number shows invalid error on blur`() {
        val vm = createViewModel()
        vm.onCardNumberChanged("4111")
        vm.onFieldFocusChanged(CardField.CARD_NUMBER, hasFocus = false)
        assertEquals(
            ValidationResult.Invalid(R.string.card_number_error),
            vm.cardNumberValidation.value
        )
    }

    @Test
    fun `invalid Luhn shows invalid error on blur`() {
        val vm = createViewModel()
        vm.onCardNumberChanged("4111111111111112")
        vm.onFieldFocusChanged(CardField.CARD_NUMBER, hasFocus = false)
        assertEquals(
            ValidationResult.Invalid(R.string.card_number_error),
            vm.cardNumberValidation.value
        )
    }

    @Test
    fun `valid card number stays Valid on blur`() {
        val vm = createViewModel()
        vm.onCardNumberChanged("4111111111111111")
        vm.onFieldFocusChanged(CardField.CARD_NUMBER, hasFocus = false)
        assertEquals(ValidationResult.Valid, vm.cardNumberValidation.value)
    }

    @Test
    fun `focus gain is a no-op`() {
        val vm = createViewModel()
        vm.onCardNumberChanged("")
        vm.onFieldFocusChanged(CardField.CARD_NUMBER, hasFocus = true)
        assertEquals(ValidationResult.Validating, vm.cardNumberValidation.value)
    }

    // endregion

    // region Card Number — correction after blur

    @Test
    fun `editing to partial input after blur error clears error`() {
        val vm = createViewModel()
        vm.onCardNumberChanged("4111111111111112")
        vm.onFieldFocusChanged(CardField.CARD_NUMBER, hasFocus = false)
        assertEquals(
            ValidationResult.Invalid(R.string.card_number_error),
            vm.cardNumberValidation.value
        )

        vm.onCardNumberChanged("4111")
        assertEquals(ValidationResult.Validating, vm.cardNumberValidation.value)
    }

    // endregion

    // region Expiration — typing state machine

    @Test
    fun `valid expiration sets Valid during typing`() {
        val vm = createViewModel()
        vm.onExpiryChanged("1228")
        assertEquals(ValidationResult.Valid, vm.expirationValidation.value)
    }

    @Test
    fun `expired date does not show error during typing`() {
        val vm = createViewModel()
        vm.onExpiryChanged("0119")
        assertEquals(ValidationResult.Validating, vm.expirationValidation.value)
    }

    @Test
    fun `invalid month does not show error during typing`() {
        val vm = createViewModel()
        vm.onExpiryChanged("1328")
        assertEquals(ValidationResult.Validating, vm.expirationValidation.value)
    }

    // endregion

    // region Expiration — blur

    @Test
    fun `empty expiration shows required error on blur`() {
        val vm = createViewModel()
        vm.onExpiryChanged("")
        vm.onFieldFocusChanged(CardField.EXPIRY, hasFocus = false)
        assertEquals(
            ValidationResult.Invalid(R.string.expiration_required),
            vm.expirationValidation.value
        )
    }

    @Test
    fun `partial expiration shows invalid error on blur`() {
        val vm = createViewModel()
        vm.onExpiryChanged("12")
        vm.onFieldFocusChanged(CardField.EXPIRY, hasFocus = false)
        assertEquals(
            ValidationResult.Invalid(R.string.expiration_error),
            vm.expirationValidation.value
        )
    }

    @Test
    fun `expired date shows invalid error on blur`() {
        val vm = createViewModel()
        vm.onExpiryChanged("0119")
        vm.onFieldFocusChanged(CardField.EXPIRY, hasFocus = false)
        assertEquals(
            ValidationResult.Invalid(R.string.expiration_error),
            vm.expirationValidation.value
        )
    }

    @Test
    fun `invalid month shows invalid error on blur`() {
        val vm = createViewModel()
        vm.onExpiryChanged("1328")
        vm.onFieldFocusChanged(CardField.EXPIRY, hasFocus = false)
        assertEquals(
            ValidationResult.Invalid(R.string.expiration_error),
            vm.expirationValidation.value
        )
    }

    @Test
    fun `valid expiration stays Valid on blur`() {
        val vm = createViewModel()
        vm.onExpiryChanged("1228")
        vm.onFieldFocusChanged(CardField.EXPIRY, hasFocus = false)
        assertEquals(ValidationResult.Valid, vm.expirationValidation.value)
    }

    // endregion

    // region CVV — typing state machine

    @Test
    fun `valid CVV sets Valid during typing`() {
        val vm = createViewModel()
        vm.onCvvChanged("123")
        assertEquals(ValidationResult.Valid, vm.cvvValidation.value)
    }

    @Test
    fun `over-length CVV does not show error during typing`() {
        val vm = createViewModel()
        vm.onCvvChanged("1234")
        assertEquals(ValidationResult.Validating, vm.cvvValidation.value)
    }

    // endregion

    // region CVV — blur

    @Test
    fun `empty CVV shows required error on blur`() {
        val vm = createViewModel()
        vm.onCvvChanged("")
        vm.onFieldFocusChanged(CardField.CVV, hasFocus = false)
        assertEquals(
            ValidationResult.Invalid(R.string.cvv_required),
            vm.cvvValidation.value
        )
    }

    @Test
    fun `partial CVV shows invalid error on blur`() {
        val vm = createViewModel()
        vm.onCvvChanged("12")
        vm.onFieldFocusChanged(CardField.CVV, hasFocus = false)
        assertEquals(
            ValidationResult.Invalid(R.string.cvv_error),
            vm.cvvValidation.value
        )
    }

    @Test
    fun `valid CVV stays Valid on blur`() {
        val vm = createViewModel()
        vm.onCvvChanged("123")
        vm.onFieldFocusChanged(CardField.CVV, hasFocus = false)
        assertEquals(ValidationResult.Valid, vm.cvvValidation.value)
    }

    // endregion

    // region CVV — brand change re-validation

    @Test
    fun `brand change from Visa to Amex invalidates 3-digit CVV`() {
        val vm = createViewModel()
        vm.onCardNumberChanged("4111111111111111")
        vm.onCvvChanged("123")
        assertEquals(ValidationResult.Valid, vm.cvvValidation.value)

        vm.onCardNumberChanged("378282246310005")
        assertEquals(ValidationResult.Validating, vm.cvvValidation.value)
    }

    @Test
    fun `brand change from Amex to Visa validates 3-digit CVV`() {
        val vm = createViewModel()
        vm.onCardNumberChanged("378282246310005")
        vm.onCvvChanged("123")
        assertEquals(ValidationResult.Validating, vm.cvvValidation.value)

        vm.onCardNumberChanged("4111111111111111")
        assertEquals(ValidationResult.Valid, vm.cvvValidation.value)
    }

    // endregion

    // region isFormValid

    @Test
    fun `isFormValid is true when all fields are Valid`() {
        val vm = createViewModel()
        vm.onCardNumberChanged("4111111111111111")
        vm.onExpiryChanged("1228")
        vm.onCvvChanged("123")
        assertTrue(vm.isFormValid.value)
    }

    @Test
    fun `isFormValid is false when any field is not Valid`() {
        val vm = createViewModel()
        vm.onCardNumberChanged("4111")
        vm.onExpiryChanged("1228")
        vm.onCvvChanged("123")
        assertFalse(vm.isFormValid.value)
    }

    // endregion
}
