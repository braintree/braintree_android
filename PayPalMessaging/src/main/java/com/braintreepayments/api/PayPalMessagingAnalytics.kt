package com.braintreepayments.api;

internal enum class PayPalMessagingAnalytics(val value: String) {
    STARTED("paypal-messaging:create-view:started"),
    FAILED("paypal-messaging:create-view:failed"),
    SUCCEEDED("paypal-messaging:create-view:succeeded");
}