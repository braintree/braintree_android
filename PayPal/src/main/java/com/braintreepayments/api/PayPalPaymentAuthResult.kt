package com.braintreepayments.api

sealed class PayPalPaymentAuthResult {

    class Success(val paymentAuthInfo: PayPalPaymentAuthResultInfo) : PayPalPaymentAuthResult()

    object NoResult : PayPalPaymentAuthResult()
}
