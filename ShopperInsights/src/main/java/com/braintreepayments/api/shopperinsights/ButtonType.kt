package com.braintreepayments.api.shopperinsights

/**
 * The button type to be displayed or presented.
 */
enum class ButtonType(internal val stringValue: String) {
    /**
     * PayPal button
     */
    PAYPAL("PayPal"),

    /**
     * Venmo button
     */
    VENMO("Venmo"),

    /**
     * Other button
     */
    OTHER("Other"),
}
