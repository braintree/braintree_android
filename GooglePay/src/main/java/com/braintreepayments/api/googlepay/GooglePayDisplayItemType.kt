package com.braintreepayments.api.googlepay

/**
 * Google Pay display item type.
 * See: https://developers.google.com/pay/api/web/reference/request-objects#DisplayItem
 */
enum class GooglePayDisplayItemType(internal val stringValue: String) {
    DISCOUNT("DISCOUNT"),
    LINE_ITEM("LINE_ITEM"),
    SHIPPING_OPTION("SHIPPING_OPTION"),
    SUBTOTAL("SUBTOTAL"),
    TAX("TAX")
}