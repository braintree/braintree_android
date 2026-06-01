package com.braintreepayments.api.uicomponents.cardfields

@Suppress("MagicNumber")
internal class ExpirationValidationUseCase {

    operator fun invoke(rawExpiration: String, currentMonth: Int, currentYear: Int): Boolean {
        if (rawExpiration.length != EXPECTED_LENGTH) return false

        val month = rawExpiration.take(2).toIntOrNull() ?: return false
        val yearSuffix = rawExpiration.drop(2).toIntOrNull() ?: return false
        val year = 2000 + yearSuffix

        return month in 1..12 &&
            (year > currentYear || (year == currentYear && month >= currentMonth))
    }

    companion object {
        private const val EXPECTED_LENGTH = 4
    }
}
