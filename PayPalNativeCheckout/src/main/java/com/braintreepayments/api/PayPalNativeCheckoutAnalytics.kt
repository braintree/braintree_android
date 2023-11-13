package com.braintreepayments.api

internal enum class PayPalNativeCheckoutAnalytics(@JvmField val event: String) {

    // Conversion Events
    TOKENIZATION_STARTED("paypal-native:tokenize:started"),
    TOKENIZATION_FAILED("paypal-native:tokenize:failed"),
    TOKENIZATION_SUCCEEDED("paypal-native:tokenize:succeeded"),
    TOKENIZATION_CANCELED("paypal-native:tokenize:canceled"),

    // Additional Detail Events
    ORDER_CREATION_FAILED("paypal-native:tokenize:order-creation:failed")
}
