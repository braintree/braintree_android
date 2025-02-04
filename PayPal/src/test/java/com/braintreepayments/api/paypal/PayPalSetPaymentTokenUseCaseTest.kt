package com.braintreepayments.api.paypal

import org.junit.Assert.assertEquals
import org.junit.Test

class PayPalSetPaymentTokenUseCaseTest {

    @Test
    fun test() {
        val payPalTokenResponseRepository = PayPalTokenResponseRepository()
        val payPalSetPaymentTokenUseCase = PayPalSetPaymentTokenUseCase(payPalTokenResponseRepository)

        payPalSetPaymentTokenUseCase.setPaymentToken("paymentToken")

        assertEquals(payPalTokenResponseRepository.paymentToken, "paymentToken")
    }
}
