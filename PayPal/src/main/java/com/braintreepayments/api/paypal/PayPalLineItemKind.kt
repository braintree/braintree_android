package com.braintreepayments.api.paypal

/**
 * The type of PayPal line item.
 *
 * [KIND_CREDIT] A line item that is a credit.
 * [KIND_DEBIT] A line item that debits.
 */
enum class PayPalLineItemKind(val stringValue: String) {
    KIND_CREDIT("credit"),
    KIND_DEBIT("debit")
}
