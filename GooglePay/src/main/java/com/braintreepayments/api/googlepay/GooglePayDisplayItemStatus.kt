package com.braintreepayments.api.googlepay

/**
 * Google Pay price variance.
 * See: https://developers.google.com/pay/api/web/reference/request-objects#DisplayItem
 */
enum class GooglePayDisplayItemStatus(internal val stringValue: String) {
    FINAL("FINAL"),
    PENDING("PENDING")
}