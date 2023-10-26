package com.braintreepayments.api

fun interface PaymentAuthRequestCallback {

    fun onPaymentAuthRequest(paymentAuthRequest: PaymentAuthRequest)
}