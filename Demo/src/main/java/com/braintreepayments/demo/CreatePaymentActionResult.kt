package com.braintreepayments.demo

sealed class CreatePaymentActionResult {
    class Success(
        val clientToken: String,
        val paymentActionId: String,
        val paymentActionStatus: String,
    ) : CreatePaymentActionResult()

    class Error(val error: Exception) : CreatePaymentActionResult()
}
