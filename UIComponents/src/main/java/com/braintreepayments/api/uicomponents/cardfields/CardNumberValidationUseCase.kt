package com.braintreepayments.api.uicomponents.cardfields

import com.braintreepayments.api.uicomponents.R

internal class CardNumberValidationUseCase {

    data class Result(
        val validation: ValidationResult,
        val detectedBrand: CardBrand
    )

    operator fun invoke(cardNumber: String): Result {
        if (cardNumber.isEmpty()) {
            return Result(validation = ValidationResult.Validating, detectedBrand = CardBrand.UNKNOWN)
        }

        val brand = CardBrand.resolveBrand(cardNumber)

        if (cardNumber.length < brand.minLength) {
            return Result(validation = ValidationResult.Validating, detectedBrand = brand)
        }

        val validLength = cardNumber.length in brand.validLengths
        val luhnValid = if (brand == CardBrand.UNIONPAY) true else LuhnValidator.isLuhnValid(cardNumber)

        val validation = if (validLength && luhnValid) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(R.string.card_number_error)
        }

        return Result(validation = validation, detectedBrand = brand)
    }
}
