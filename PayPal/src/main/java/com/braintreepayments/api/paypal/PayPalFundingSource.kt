package com.braintreepayments.api.paypal

/**
 * Funding sources available when creating a PayPal payment.
 */
internal enum class PayPalFundingSource(val value: String) {
    /**
     * Standard PayPal balance or linked bank account funding.
     */
    PAYPAL("paypal"),

    /**
     * PayPal Pay Later BNPL products for short-term installments (e.g. Pay in 4,
     * Pay Monthly in the US).
     */
    PAY_LATER("paylater"),

    /**
     * PayPal Credit revolving line of credit.
     */
    CREDIT("credit"),
}

/**
 * Returns the [PayPalFundingSource] of a PayPalRequest based on its type and parameters
 */
internal fun PayPalRequest.getFundingSource(): PayPalFundingSource = when {
    this is PayPalCheckoutRequest && shouldOfferPayLater -> PayPalFundingSource.PAY_LATER
    shouldOfferCredit -> PayPalFundingSource.CREDIT
    else -> PayPalFundingSource.PAYPAL
}
