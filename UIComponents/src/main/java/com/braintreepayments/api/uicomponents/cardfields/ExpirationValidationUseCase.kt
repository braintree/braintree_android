package com.braintreepayments.api.uicomponents.cardfields

import com.braintreepayments.api.uicomponents.R

@Suppress("MagicNumber")
internal class ExpirationValidationUseCase {

    operator fun invoke(rawExpiration: String, currentMonth: Int, currentYear: Int): ValidationResult {
        if (rawExpiration.length < EXPECTED_LENGTH) return ValidationResult.Validating

        val month = rawExpiration.take(2).toIntOrNull()
        val yearSuffix = rawExpiration.drop(2).toIntOrNull()

        if (rawExpiration.length > EXPECTED_LENGTH || month == null || yearSuffix == null) {
            return ValidationResult.Invalid(R.string.expiration_error)
        }

        val year = 2000 + yearSuffix
        val isNotExpired = year > currentYear || (year == currentYear && month >= currentMonth)

        return if (month in 1..12 && isNotExpired) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(R.string.expiration_error)
        }
    }

    companion object {
        private const val EXPECTED_LENGTH = 4
    }
}
