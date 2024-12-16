package com.braintreepayments.api.shopperinsights

/**
The button type to be displayed or presented.
 */
enum class ButtonType(internal val stringValue: String) {
    /**
     * PayPal button
     */
    PAYPAL("paypal"),

    /**
     * Venmo button
     */
    VENMO("venmo"),

    /**
     * Other button
     */
    OTHER("other"),
}