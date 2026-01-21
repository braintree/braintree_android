package com.braintreepayments.api.googlepay

/**
 * Options for the checkout flow.
 *
 * @property stringValue The string value of the checkout option.
 */
enum class GooglePayCheckoutOption(val stringValue: String) {
    /**
     * Standard checkout option.
     */
    DEFAULT("DEFAULT"),

    CONTINUE_TO_REVIEW("CONTINUE_TO_REVIEW"),

    /**
     * Complete immediate purchase.
     */
    COMPLETE_IMMEDIATE_PURCHASE("COMPLETE_IMMEDIATE_PURCHASE");

    override fun toString(): String {
        return stringValue
    }
}
