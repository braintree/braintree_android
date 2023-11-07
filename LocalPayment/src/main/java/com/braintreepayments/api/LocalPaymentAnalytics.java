package com.braintreepayments.api;

enum LocalPaymentAnalytics {

    // Conversion Events
    PAYMENT_STARTED("local-payment:start-payment:started"),
    PAYMENT_SUCCEEDED("local-payment:start-payment:succeeded"),
    PAYMENT_FAILED("local-payment:start-payment:failed"),
    PAYMENT_CANCELED("local-payment:start-payment:browser-login:canceled"),

    // Browser Presentation Events
    BROWSER_PRESENTATION_SUCCEEDED("local-payment:start-payment:browser-presentation:succeeded"),
    BROWSER_PRESENTATION_FAILED("local-payment:start-payment:browser-presentation:failed"),

    // Browser Login Events
    BROWSER_LOGIN_FAILED("local-payment:start-payment:browser-login:failed"),

    // Network Connection Event
    PAYMENT_NETWORK_CONNECTION_LOST("local-payment:start-payment:network-connection:failed");

    private final String event;

    LocalPaymentAnalytics(String event) {
        this.event = event;
    }

    String getEvent() {
        return event;
    }
}
