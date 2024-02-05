package com.braintreepayments.api

internal object PayPalAnalytics {

    // Conversion Events
    const val TOKENIZATION_STARTED = "paypal:tokenize:started"
    const val TOKENIZATION_FAILED = "paypal:tokenize:failed"
    const val TOKENIZATION_SUCCEEDED = "paypal:tokenize:succeeded"
    const val BROWSER_LOGIN_CANCELED = "paypal:tokenize:browser-login:canceled"

    // Browser Presentation Events
    const val BROWSER_SWITCH_SUCCEEDED = "paypal:tokenize:browser-presentation:succeeded"
    const val BROWSER_SWITCH_FAILED = "paypal:tokenize:browser-presentation:failed"
}
