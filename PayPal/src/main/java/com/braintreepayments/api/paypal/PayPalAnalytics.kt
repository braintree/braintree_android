package com.braintreepayments.api.paypal

internal object PayPalAnalytics {

    // Conversion Events
    const val TOKENIZATION_STARTED = "paypal:tokenize:started"
    const val TOKENIZATION_FAILED = "paypal:tokenize:failed"
    const val TOKENIZATION_SUCCEEDED = "paypal:tokenize:succeeded"
    const val BROWSER_LOGIN_CANCELED = "paypal:tokenize:browser-login:canceled"

    // Edit FI Events
    const val EDIT_FI_STARTED = "paypal:edit:started"
    const val EDIT_FI_BROWSER_PRESENTATION_SUCCEEDED = "paypal:edit:browser-presentation:succeeded"
    const val EDIT_FI_BROWSER_PRESENTATION_FAILED = "paypal:edit:browser-presentation:failed"
    const val EDIT_FI_BROWSER_LOGIN_CANCELED = "paypal:edit:browser-login:canceled"
    const val EDIT_FI_FAILED = "paypal:edit:failed"
    const val EDIT_FI_SUCCEEDED = "paypal:edit:succeeded"
}
