package com.braintreepayments.api.paypalsavedpaymentmethod

/**
 * Determines which funding instrument the Braintree GraphQL API resolves, and therefore which identity field is
 * required.
 *
 * @property rawValue The `PayPalFundingInstrumentFetchType` GraphQL enum value sent in the request.
 */
internal enum class PayPalFundingInstrumentFetchType(val rawValue: String) {

    /**
     * The default funding instrument vaulted on the buyer's billing agreement. Resolved from the client token's
     * payment method ID JWT.
     */
    BUYER_DEFAULT_BILLING_AGREEMENT("STICKY_FI"),

    /**
     * The funding instrument the buyer selected while approving a checkout. Resolved from that order's ID.
     */
    BUYER_UPDATED_BILLING_AGREEMENT("FI_FROM_APPROVED_CHECKOUT")
}
