package com.braintreepayments.api.paypal

enum class PayPalLandingPageType(val stringValue: String) {
    /**
     * A non-PayPal account landing page is used.
     */
    LANDING_PAGE_TYPE_BILLING("billing"),

    /**
     * A PayPal account login page is used.
     */
    LANDING_PAGE_TYPE_LOGIN("login")
}
