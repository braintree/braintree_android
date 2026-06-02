package com.braintreepayments.api.uicomponents.cardfields

import com.braintreepayments.api.uicomponents.R

@Suppress("MagicNumber")
internal class ExpirationValidationUseCase {

    operator fun invoke(rawExpiration: String, currentMonth: Int, currentYear: Int): ValidationResult {
        if (rawExpiration.length < EXPECTED_LENGTH) return ValidationResult.Validating
        if (rawExpiration.length > EXPECTED_LENGTH) return ValidationResult.Invalid(R.string.expiration_error)

        val month = rawExpiration.take(2).toIntOrNull()
            ?: return ValidationResult.Invalid(R.string.expiration_error)
        val yearSuffix = rawExpiration.drop(2).toIntOrNull()
            ?: return ValidationResult.Invalid(R.string.expiration_error)
        val year = 2000 + yearSuffix

        return if (month in 1..12 && (year > currentYear || (year == currentYear && month >= currentMonth))) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(R.string.expiration_error)
        }
    }

    companion object {
        private const val EXPECTED_LENGTH = 4
    }
}
