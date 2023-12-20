package com.braintreepayments.api

internal object GooglePayAnalytics {

    // Payment Request Events

    const val PAYMENT_REQUEST_STARTED = "google-pay:payment-request:started"
    const val PAYMENT_REQUEST_FAILED = "google-pay:payment-request:failed"
    const val PAYMENT_REQUEST_SUCCEEDED = "google-pay:payment-request:succeeded"

    // Tokenize Events

    const val TOKENIZE_STARTED = "google-pay:tokenize:started"
    const val TOKENIZE_FAILED = "google-pay:tokenize:failed"
    const val TOKENIZE_SUCCEEDED = "google-pay:tokenize:succeeded"

    const val PAYMENT_SHEET_CANCELED = "google-pay:payment-sheet:canceled"
}
