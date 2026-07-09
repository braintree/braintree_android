package com.braintreepayments.api.paypal

internal object PayPalAnalytics {

    // Conversion Events
    const val TOKENIZATION_STARTED = "paypal:tokenize:started"
    const val TOKENIZATION_FAILED = "paypal:tokenize:failed"
    const val TOKENIZATION_SUCCEEDED = "paypal:tokenize:succeeded"

    const val BROWSER_PRESENTATION_STARTED = "paypal:tokenize:browser-presentation:started"
    const val BROWSER_LOGIN_CANCELED = "paypal:tokenize:browser-login:canceled"
    const val BROWSER_PRESENTATION_SUCCEEDED = "paypal:tokenize:browser-presentation:succeeded"
    const val BROWSER_PRESENTATION_FAILED = "paypal:tokenize:browser-presentation:failed"

    // Additional Conversion events
    const val HANDLE_RETURN_STARTED = "paypal:tokenize:handle-return:started"
    const val HANDLE_RETURN_SUCCEEDED = "paypal:tokenize:handle-return:succeeded"
    const val HANDLE_RETURN_FAILED = "paypal:tokenize:handle-return:failed"
    const val HANDLE_RETURN_NO_RESULT = "paypal:tokenize:handle-return:no-result"

    // App Switch events
    const val APP_SWITCH_STARTED = "paypal:tokenize:app-switch:started"
    const val APP_SWITCH_SUCCEEDED = "paypal:tokenize:app-switch:succeeded"
    const val APP_SWITCH_FAILED = "paypal:tokenize:app-switch:failed"
    const val APP_SWITCH_CANCELED = "paypal:tokenize:app-switch:canceled"

    // Auto-Link events — tokenization via stored BA token when App Link return fails
    const val AUTO_LINK_TOKENIZE_STARTED = "paypal:tokenize:auto-link:started"
    const val AUTO_LINK_TOKENIZE_SUCCEEDED = "paypal:tokenize:auto-link:succeeded"
    const val AUTO_LINK_TOKENIZE_FAILED = "paypal:tokenize:auto-link:failed"
    const val AUTO_LINK_EXPIRED = "paypal:tokenize:auto-link:expired"

    // Auto-Link via handle-return — completeRequest returned NoResult but a session is pending
    const val AUTO_LINK_HANDLE_RETURN_PENDING = "paypal:tokenize:auto-link:handle-return:pending"

    // Auto-Link via re-click — user re-taps PayPal before normal return completes
    const val AUTO_LINK_RECLICK_STARTED = "paypal:tokenize:auto-link:reclick:started"
    const val AUTO_LINK_RECLICK_SUCCEEDED = "paypal:tokenize:auto-link:reclick:succeeded"
    const val AUTO_LINK_RECLICK_FAILED = "paypal:tokenize:auto-link:reclick:failed"
}
