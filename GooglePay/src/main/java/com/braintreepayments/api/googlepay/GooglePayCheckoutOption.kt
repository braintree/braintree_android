package com.braintreepayments.api.googlepay

/**
 * Options for the checkout flow.
 *
 * @property stringValue The string value of the checkout option.
 */
enum class GooglePayCheckoutOption(val stringValue: String) {
    /**
     * Google Pay automatically determines whether to show "Continue" or "Pay".
     */
    DEFAULT("DEFAULT"),

    /**
     * The payment sheet button is labeled "Review Order".
     */
    CONTINUE_TO_REVIEW("CONTINUE_TO_REVIEW"),

    /**
     * The payment sheet button is labeled "Pay now".
     */
    COMPLETE_IMMEDIATE_PURCHASE("COMPLETE_IMMEDIATE_PURCHASE")
}
