package com.braintreepayments.demo

fun interface CreatePaymentActionCallback {
    fun onResult(result: CreatePaymentActionResult)
}
