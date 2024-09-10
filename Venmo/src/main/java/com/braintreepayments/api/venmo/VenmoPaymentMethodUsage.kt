package com.braintreepayments.api.venmo

/**
 * Usage type for the tokenized Venmo account.
 */
enum class VenmoPaymentMethodUsage {

    /**
     * The Venmo payment will be authorized for a one-time payment and cannot be vaulted.
     */
    SINGLE_USE,

    /**
     * The Venmo payment will be authorized for future payments and can be vaulted.
     */
    MULTI_USE
}
