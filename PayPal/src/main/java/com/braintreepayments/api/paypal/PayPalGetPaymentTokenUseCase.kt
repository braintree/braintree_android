package com.braintreepayments.api.paypal

class PayPalGetPaymentTokenUseCase(private val payPalTokenResponseRepository: PayPalTokenResponseRepository) {

    operator fun invoke(): String? {
        return payPalTokenResponseRepository.paymentToken
    }
}
