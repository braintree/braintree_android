package com.braintreepayments.api

sealed class PaymentResult {

    class Success(val nonce: PaymentMethodNonce): PaymentResult()

    class Failure(val error: Exception) : PaymentResult()

    class Cancel : PaymentResult()

}