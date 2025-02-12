package com.braintreepayments.api.paypal

class PayPalSetPaymentTokenUseCase(private val payPalTokenResponseRepository: PayPalTokenResponseRepository) {

    fun setPaymentToken(paymentToken: String?) {
        payPalTokenResponseRepository.paymentToken = paymentToken
    }
}
