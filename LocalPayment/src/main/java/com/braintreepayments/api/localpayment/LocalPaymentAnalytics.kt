package com.braintreepayments.api.localpayment

internal object LocalPaymentAnalytics {

    // Conversion Events
    const val PAYMENT_STARTED = "local-payment:start-payment:started"
    const val PAYMENT_SUCCEEDED = "local-payment:start-payment:succeeded"
    const val PAYMENT_FAILED = "local-payment:start-payment:failed"
    const val PAYMENT_CANCELED = "local-payment:start-payment:browser-login:canceled"

    // Browser Presentation Events
    const val BROWSER_SWITCH_SUCCEEDED =
        "local-payment:start-payment:browser-presentation:succeeded"
}
