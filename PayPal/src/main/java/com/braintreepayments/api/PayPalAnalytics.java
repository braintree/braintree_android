package com.braintreepayments.api;

enum PayPalAnalytics {

    // Tokenize Events
    TOKENIZATION_STARTED("paypal:tokenize:started"),
    TOKENIZATION_FAILED("paypal:tokenize:failed"),
    TOKENIZATION_SUCCEEDED("paypal:tokenize:succeeded"),
    TOKENIZATION_NETWORK_CONNECTION_FAILED("paypal:tokenize:network-connection:failed"),

    // Browser Presentation Events
    BROWSER_PRESENTATION_SUCCEEDED("paypal:tokenize:browser-presentation:succeeded"),
    BROWSER_PRESENTATION_FAILED("paypal:tokenize:browser-presentation:failed"),

    // Browser Login Events
    BROWSER_LOGIN_CANCELED("paypal:tokenize:browser-login:canceled");

    private final String event;

    PayPalAnalytics(String event) {
        this.event = event;
    }

    String getEvent() {
        return event;
    }
}

