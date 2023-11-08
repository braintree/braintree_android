package com.braintreepayments.api

internal enum class PayPalAnalytics(@JvmField val event: String) {

    // Tokenize Events
    TOKENIZATION_STARTED("paypal:tokenize:started"),
    TOKENIZATION_FAILED("paypal:tokenize:failed"),
    TOKENIZATION_SUCCEEDED("paypal:tokenize:succeeded"),

    // Browser Presentation Events
    BROWSER_SWITCH_SUCCEEDED("paypal:tokenize:browser-presentation:succeeded"),
    BROWSER_SWITCH_FAILED("paypal:tokenize:browser-presentation:failed"),

    // Browser Login Events
    BROWSER_LOGIN_CANCELED("paypal:tokenize:browser-login:canceled")
}
