package com.braintreepayments.api.visacheckout

internal object VisaCheckoutAnalytics {

    // Conversion Events
    const val TOKENIZE_STARTED = "visa-checkout:tokenize:started"
    const val TOKENIZE_SUCCEEDED = "visa-checkout:tokenize:succeeded"
    const val TOKENIZE_FAILED = "visa-checkout:tokenize:failed"
}