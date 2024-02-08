package com.braintreepayments.api

sealed class SEPADirectDebitPaymentAuthResult {

    class Success(val paymentAuthInfo: SEPADirectDebitPaymentAuthResultInfo) : SEPADirectDebitPaymentAuthResult()

    object NoResult : SEPADirectDebitPaymentAuthResult()
}
