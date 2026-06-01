package com.braintreepayments.api.uicomponents.cardfields

internal class CvvValidationUseCase {

    operator fun invoke(cvv: String, cardBrand: CardBrand): Boolean {
        return cvv.length == cardBrand.cvvLength
    }
}
