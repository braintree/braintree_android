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
     * PayPal Pay Later button
     */
    PAYPAL_PAY_LATER("PayPal_Pay_Later"),

    /**
     * Other button
     */
    OTHER("Other")
}
