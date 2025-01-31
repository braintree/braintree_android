package com.braintreepayments.api.paypal

import androidx.annotation.RestrictTo

/**
 * The payment intent in the PayPal Checkout flow
 */
enum class PayPalPaymentIntent(internal val stringValue: String) {

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
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        fun fromString(string: String?): PayPalPaymentIntent? {
            return entries.firstOrNull { it.stringValue.equals(string, ignoreCase = true) }
        }
    }
}
