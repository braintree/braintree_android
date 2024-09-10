package com.braintreepayments.api.venmo

/**
 * The type of Venmo line item.
 */
enum class VenmoLineItemKind {
    /**
     * A line item that is a credit.
     */
    CREDIT,

    /**
     * A line item that debits.
     */
    DEBIT
}
