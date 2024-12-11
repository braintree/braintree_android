package com.braintreepayments.api.shopperinsights

/*
The button type to be displayed or presented
 */
enum class ButtonType(internal val stringValue: String) {
    PAYPAL("paypal"),
    VENMO("venmo"),
    OTHER("other"),
}