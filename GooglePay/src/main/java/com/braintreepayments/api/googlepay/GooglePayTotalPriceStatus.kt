package com.braintreepayments.api.googlepay

/**
 * The status of the Google Pay transaction total price.
 */
enum class GooglePayTotalPriceStatus(internal val stringValue: String) {

    /**
     * The total price is an estimated price and might still change.
     */
    TOTAL_PRICE_STATUS_ESTIMATED("ESTIMATED"),

    /**
     * The total price is the final total price of the transaction and will not change.
     */
    TOTAL_PRICE_STATUS_FINAL("FINAL")
}
