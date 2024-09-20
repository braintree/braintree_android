package com.braintreepayments.api.paypal

internal object PayPalAnalytics {

    // Conversion Events
    const val TOKENIZATION_STARTED = "paypal:tokenize:started"
    const val TOKENIZATION_FAILED = "paypal:tokenize:failed"
    const val TOKENIZATION_SUCCEEDED = "paypal:tokenize:succeeded"
    const val BROWSER_LOGIN_CANCELED = "paypal:tokenize:browser-login:canceled"

    // Additional Conversion events
    const val HANDLE_RETURN_STARTED = "paypal:tokenize:handle-return:started"

    // App Switch events
    const val APP_SWITCH_STARTED = "paypal:tokenize:app-switch:started"
    const val APP_SWITCH_SUCCEEDED = "paypal:tokenize:app-switch:succeeded"
    const val APP_SWITCH_FAILED = "paypal:tokenize:app-switch:failed"
}
