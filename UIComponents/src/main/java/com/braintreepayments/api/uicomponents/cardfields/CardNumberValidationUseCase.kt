package com.braintreepayments.api.uicomponents.cardfields

internal class CardNumberValidationUseCase {

    data class Result(
        val isValid: Boolean,
        val detectedBrand: CardBrand
    )

    operator fun invoke(cardNumber: String): Result {
        if (cardNumber.isEmpty()) {
            return Result(isValid = false, detectedBrand = CardBrand.UNKNOWN)
        }

        val brand = CardBrand.resolveBrand(cardNumber)

        val validLength = cardNumber.length in brand.validLengths
        val luhnValid = if (brand == CardBrand.UNIONPAY) true else LuhnValidator.isLuhnValid(cardNumber)

        return Result(
            isValid = validLength && luhnValid,
            detectedBrand = brand
        )
    }
}
