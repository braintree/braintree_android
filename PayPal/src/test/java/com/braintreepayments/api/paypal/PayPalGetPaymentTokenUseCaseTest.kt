package com.braintreepayments.api.paypal

import org.junit.Assert.assertEquals
import org.junit.Test

class PayPalGetPaymentTokenUseCaseTest {

    @Test
    fun test() {
        val payPalTokenResponseRepository = PayPalTokenResponseRepository()
        val payPalGetPaymentTokenUseCase = PayPalGetPaymentTokenUseCase(payPalTokenResponseRepository)

        val sampleToken = "Payment token"
        payPalTokenResponseRepository.paymentToken = sampleToken

        assertEquals(payPalGetPaymentTokenUseCase(), sampleToken)
    }
}
