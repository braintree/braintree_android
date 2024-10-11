package com.braintreepayments.api.paypal

/**
 * The type of PayPal line item.
 *
 * [CREDIT] A line item that is a credit.
 * [DEBIT] A line item that debits.
 */
enum class PayPalLineItemKind(internal val stringValue: String) {
    CREDIT("credit"),
    DEBIT("debit")
}
