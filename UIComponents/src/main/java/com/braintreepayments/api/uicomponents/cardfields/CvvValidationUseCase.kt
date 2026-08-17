package com.braintreepayments.api.uicomponents.cardfields

import com.braintreepayments.api.uicomponents.R

internal class CvvValidationUseCase {

    operator fun invoke(cvv: String, cardBrand: CardBrand): ValidationResult {
        return when {
            cvv.isEmpty() -> ValidationResult.Validating
            cvv.length < cardBrand.minCvvLength -> ValidationResult.Validating
            cvv.length <= cardBrand.cvvLength -> ValidationResult.Valid
            else -> ValidationResult.Invalid(R.string.cvv_error)
        }
    }
}
