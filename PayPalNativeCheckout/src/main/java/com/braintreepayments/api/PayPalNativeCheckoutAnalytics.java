package com.braintreepayments.api;

enum PayPalNativeCheckoutAnalytics {

    // Conversion Events
    TOKENIZATION_STARTED( "paypal-native:tokenize:started"),
    TOKENIZATION_FAILED("paypal-native:tokenize:failed"),
    TOKENIZATION_SUCCEEDED("paypal-native:tokenize:succeeded"),
    TOKENIZATION_CANCELED("paypal-native:tokenize:canceled"),

    // Additional Detail Events
    ORDER_CREATION_FAILED("paypal-native:tokenize:order-creation:failed");

    private final String event;

    PayPalNativeCheckoutAnalytics(String event) {
        this.event = event;
    }

    String getEvent() {
        return event;
    }
}
