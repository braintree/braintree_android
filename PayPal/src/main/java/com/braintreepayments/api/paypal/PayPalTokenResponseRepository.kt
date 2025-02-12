package com.braintreepayments.api.paypal

class PayPalTokenResponseRepository {
    var paymentToken: String? = null

    companion object {

        /**
         * Singleton instance of the TokenResponseRepository.
         */
        val instance: PayPalTokenResponseRepository by lazy { PayPalTokenResponseRepository() }
    }
}
