package com.braintreepayments.api

internal enum class LocalPaymentAnalytics(@JvmField val event: String) {

    // Conversion Events
    PAYMENT_STARTED("local-payment:start-payment:started"),
    PAYMENT_SUCCEEDED("local-payment:start-payment:succeeded"),
    PAYMENT_FAILED("local-payment:start-payment:failed"),
    PAYMENT_CANCELED("local-payment:start-payment:browser-login:canceled"),

    // Browser Presentation Events
    BROWSER_SWITCH_SUCCEEDED("local-payment:start-payment:browser-presentation:succeeded"),
    BROWSER_SWITCH_FAILED("local-payment:start-payment:browser-presentation:failed"),

    // Browser Login Events
    BROWSER_LOGIN_FAILED("local-payment:start-payment:browser-login:failed")
}
