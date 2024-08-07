package com.braintreepayments.api.paypal

/**
 * The payment intent in the PayPal Checkout flow
 */
enum class PayPalPaymentIntent(val stringValue: String) {

    /**
     * Payment intent to create an order
     */
    ORDER("order"),

    /**
     * Payment intent for immediate payment
     */
    SALE("sale"),

    /**
     * Payment intent to authorize a payment for capture later
     */
    AUTHORIZE("authorize");

    companion object {
        @JvmStatic
        fun fromString(string: String?): PayPalPaymentIntent? {
            return PayPalPaymentIntent.values().firstOrNull { it.stringValue == string }
        }
    }
}
