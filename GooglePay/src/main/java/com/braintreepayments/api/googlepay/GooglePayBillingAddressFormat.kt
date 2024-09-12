package com.braintreepayments.api.googlepay


/**
 * The billing address format required for the transaction
 */
enum class GooglePayBillingAddressFormat {
    /**
     * Default value. Name, country code, and postal code.
     */
    MIN,

    /**
     * Name, street address, locality, region, country code, and postal code.
     */
    FULL
}