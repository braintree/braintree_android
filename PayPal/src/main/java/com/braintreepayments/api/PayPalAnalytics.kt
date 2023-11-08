package com.braintreepayments.api

internal enum class PayPalAnalytics(@JvmField val event: String) {

    // Conversion Events
    TOKENIZATION_STARTED("paypal:tokenize:started"),
    TOKENIZATION_FAILED("paypal:tokenize:failed"),
    TOKENIZATION_SUCCEEDED("paypal:tokenize:succeeded"),
    BROWSER_LOGIN_CANCELED("paypal:tokenize:browser-login:canceled"),

    // Browser Presentation Events
    BROWSER_SWITCH_SUCCEEDED("paypal:tokenize:browser-presentation:succeeded"),
    BROWSER_SWITCH_FAILED("paypal:tokenize:browser-presentation:failed")

}
