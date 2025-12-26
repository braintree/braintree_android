package com.braintreepayments.api.paypal

enum class PayPalFundingSource(val value: String) {
    PAYPAL("paypal"),
    PAY_LATER("paylater"),
    CREDIT("credit"),
}

/**
 * Returns the [PayPalFundingSource] of a PayPalRequest based on its type and parameters
 */
fun PayPalRequest.getFundingSource(): PayPalFundingSource = when {
    this is PayPalCheckoutRequest && shouldOfferPayLater -> PayPalFundingSource.PAY_LATER
    shouldOfferCredit -> PayPalFundingSource.CREDIT
    else -> PayPalFundingSource.PAYPAL
}