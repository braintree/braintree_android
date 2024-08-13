package com.braintreepayments.api.paypal

internal object PayPalAnalytics {

    // Conversion Events
    const val TOKENIZATION_STARTED = "paypal:tokenize:started"
    const val TOKENIZATION_FAILED = "paypal:tokenize:failed"
    const val TOKENIZATION_SUCCEEDED = "paypal:tokenize:succeeded"
    const val BROWSER_LOGIN_CANCELED = "paypal:tokenize:browser-login:canceled"

    const val EDITFI_STARTED = "paypal:edit:started"
    const val EDITFI_BROWSER_PRESENTATION_SUCCEEDED = "paypal:edit:browser-presentation:succeeded"
    const val EDITFI_BROWSER_PRESENTATION_FAILED = "paypal:edit:browser-presentation:failed"
    const val EDITFI_BROWSER_LOGIN_ALERT_CANCELED = "paypal:edit:browser-login:alert-canceled"
    const val EDITFI_BROWSER_LOGIN_CANCELED = "paypal:edit:browser-login:canceled"
    const val EDITFI_FAILED = "paypal:edit:failed"
    const val EDITFI_SUCCEEDED = "paypal:edit:succeeded"
}
