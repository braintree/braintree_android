package com.braintreepayments.api

internal enum class GooglePayAnalytics(@JvmField val event: String) {

    PAYMENT_REQUEST_STARTED("google-pay:payment-request:started"),
    PAYMENT_REQUEST_FAILED("google-pay:payment-request:failed"),
    PAYMENT_REQUEST_SUCCEEDED("google-pay:payment-request:succeeded"),

    TOKENIZE_STARTED("google-pay:tokenize:started"),
    TOKENIZE_FAILED("google-pay:tokenize:failed"),
    TOKENIZE_SUCCEEDED("google-pay:tokenize:succeeded")
}
