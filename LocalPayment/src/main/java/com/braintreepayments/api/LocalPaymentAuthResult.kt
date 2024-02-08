package com.braintreepayments.api

sealed class LocalPaymentAuthResult {

    class Success(val paymentAuthInfo: LocalPaymentAuthResultInfo) : LocalPaymentAuthResult()

    object NoResult : LocalPaymentAuthResult()
}
