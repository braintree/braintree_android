package com.braintreepayments.api.uicomponents.cardfields

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import com.braintreepayments.api.uicomponents.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

/**
 * Orchestrates validation for card number, expiration, and CVV fields.
 *
 * Exposes [StateFlow]s of [ValidationResult] per field that the UI observes to display errors.
 * Uses a state machine during typing so errors never appear while the user is actively editing —
 * only [ValidationResult.Valid] propagates immediately. On blur, incomplete fields resolve to
 * [ValidationResult.Invalid] with either a "required" or "invalid" error message.
 */
@Suppress("TooManyFunctions")
internal class CardFieldsViewModel(
    private val cardNumberValidationUseCase: CardNumberValidationUseCase = CardNumberValidationUseCase(),
    private val expirationValidationUseCase: ExpirationValidationUseCase = ExpirationValidationUseCase(),
    private val cvvValidationUseCase: CvvValidationUseCase = CvvValidationUseCase(),
    private val currentDateProvider: () -> Pair<Int, Int> = {
        val cal = Calendar.getInstance()
        Pair(cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
    }
) : ViewModel() {

    private val _cardNumberValidation = MutableStateFlow<ValidationResult>(ValidationResult.Validating)
    val cardNumberValidation: StateFlow<ValidationResult> = _cardNumberValidation

    private val _expirationValidation = MutableStateFlow<ValidationResult>(ValidationResult.Validating)
    val expirationValidation: StateFlow<ValidationResult> = _expirationValidation

    private val _cvvValidation = MutableStateFlow<ValidationResult>(ValidationResult.Validating)
    val cvvValidation: StateFlow<ValidationResult> = _cvvValidation

    private val _detectedCardBrand = MutableStateFlow(CardBrand.UNKNOWN)
    val detectedCardBrand: StateFlow<CardBrand> = _detectedCardBrand

    private val _isFormValid = MutableStateFlow(false)
    val isFormValid: StateFlow<Boolean> = _isFormValid

    private var currentCardNumber = ""
    private var currentExpiration = ""
    private var currentCvv = ""

    fun onCardNumberChanged(cardNumber: String) {
        currentCardNumber = cardNumber
        val result = cardNumberValidationUseCase(cardNumber)
        val previousBrand = _detectedCardBrand.value
        _detectedCardBrand.value = result.detectedBrand

        suppressErrorDuringTyping(_cardNumberValidation, result.validation)

        if (previousBrand != result.detectedBrand) {
            revalidateCvv()
        }

        updateIsValid()
    }

    fun onExpiryChanged(rawExpiration: String) {
        currentExpiration = rawExpiration
        val (currentMonth, currentYear) = currentDateProvider()
        val result = expirationValidationUseCase(rawExpiration, currentMonth, currentYear)

        suppressErrorDuringTyping(_expirationValidation, result)

        updateIsValid()
    }

    fun onCvvChanged(cvv: String) {
        currentCvv = cvv
        val result = cvvValidationUseCase(cvv, _detectedCardBrand.value)

        suppressErrorDuringTyping(_cvvValidation, result)

        updateIsValid()
    }

    /**
     * Call when a field gains or loses focus. On focus loss (blur), re-validates and resolves
     * [ValidationResult.Validating] to [ValidationResult.Invalid] with the appropriate error.
     */
    fun onFieldFocusChanged(field: CardField, hasFocus: Boolean) {
        if (hasFocus) return

        when (field) {
            CardField.CARD_NUMBER -> {
                val result = cardNumberValidationUseCase(currentCardNumber)
                _detectedCardBrand.value = result.detectedBrand
                _cardNumberValidation.value = resolveOnBlur(
                    useCaseResult = result.validation,
                    isEmpty = currentCardNumber.isEmpty(),
                    requiredRes = R.string.card_number_required,
                    errorRes = R.string.card_number_error
                )
            }
            CardField.EXPIRY -> {
                val (currentMonth, currentYear) = currentDateProvider()
                _expirationValidation.value = resolveOnBlur(
                    useCaseResult = expirationValidationUseCase(currentExpiration, currentMonth, currentYear),
                    isEmpty = currentExpiration.isEmpty(),
                    requiredRes = R.string.expiration_required,
                    errorRes = R.string.expiration_error
                )
            }
            CardField.CVV -> {
                _cvvValidation.value = resolveOnBlur(
                    useCaseResult = cvvValidationUseCase(currentCvv, _detectedCardBrand.value),
                    isEmpty = currentCvv.isEmpty(),
                    requiredRes = R.string.cvv_required,
                    errorRes = R.string.cvv_error
                )
            }
        }

        updateIsValid()
    }

    /** Re-runs CVV validation when the detected card brand changes (e.g., Amex requires 4 digits). */
    private fun revalidateCvv() {
        val result = cvvValidationUseCase(currentCvv, _detectedCardBrand.value)
        if (_cvvValidation.value is ValidationResult.Valid && result is ValidationResult.Validating) {
            _cvvValidation.value = ValidationResult.Invalid(R.string.cvv_error)
        } else if (_cvvValidation.value is ValidationResult.Valid) {
            _cvvValidation.value = result
        } else {
            suppressErrorDuringTyping(_cvvValidation, result)
        }
    }

    /**
     * State machine for typing: only [ValidationResult.Valid] propagates immediately.
     * Any other result clears a previous error or valid state back to [ValidationResult.Validating],
     * ensuring errors are never shown while the user is actively editing.
     */
    private fun suppressErrorDuringTyping(
        flow: MutableStateFlow<ValidationResult>,
        useCaseResult: ValidationResult
    ) {
        if (useCaseResult is ValidationResult.Valid) {
            flow.value = ValidationResult.Valid
        } else if (flow.value !is ValidationResult.Validating) {
            flow.value = ValidationResult.Validating
        }
    }

    /** Converts [ValidationResult.Validating] to [ValidationResult.Invalid] on blur, choosing
     *  "required" if the field is empty or "invalid" if it has partial input. */
    private fun resolveOnBlur(
        useCaseResult: ValidationResult,
        isEmpty: Boolean,
        @StringRes requiredRes: Int,
        @StringRes errorRes: Int
    ): ValidationResult = when {
        useCaseResult is ValidationResult.Valid -> useCaseResult
        useCaseResult is ValidationResult.Invalid -> useCaseResult
        isEmpty -> ValidationResult.Invalid(requiredRes)
        else -> ValidationResult.Invalid(errorRes)
    }

    /** Sets [isFormValid] to true when each of the input fields are valid*/
    private fun updateIsValid() {
        _isFormValid.value = _cardNumberValidation.value is ValidationResult.Valid &&
            _expirationValidation.value is ValidationResult.Valid &&
            _cvvValidation.value is ValidationResult.Valid
    }
}
