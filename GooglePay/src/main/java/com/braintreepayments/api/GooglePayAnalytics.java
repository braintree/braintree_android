package com.braintreepayments.api;

enum GooglePayAnalytics {

    // Payment Data Request Events
    PAYMENT_REQUEST_STARTED("google-pay:payment-request:started"),
    PAYMENT_REQUEST_FAILED("google-pay:payment-request:failed"),
    PAYMENT_REQUEST_SUCCEEDED("google-pay:payment-request:succeeded"),

    // Tokenize Events
    TOKENIZE_STARTED("google-pay:tokenize:started"),
    TOKENIZE_FAILED("google-pay:tokenize:failed"),
    TOKENIZE_NETWORK_CONNECTION_LOST("google-pay:tokenize:network-connection:failed"),
    TOKENIZE_SUCCEEDED("google-pay:tokenize:succeeded");

    private final String event;

    GooglePayAnalytics(String event) {
        this.event = event;
    }

    String getEvent() {
        return event;
    }
}
