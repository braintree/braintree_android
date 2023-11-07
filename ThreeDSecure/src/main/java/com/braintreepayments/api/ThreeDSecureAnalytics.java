package com.braintreepayments.api;

enum ThreeDSecureAnalytics {

    // Conversion Events
    VERIFY_STARTED("3ds:verify:started"),
    VERIFY_SUCCEEDED("3ds:verify:succeeded"),
    VERIFY_FAILED("3ds:verify:failed"),

    // cardinal sdk returns a cancellation result
    VERIFY_CANCELED("3ds:verify:canceled"),

    // Lookup Events
    LOOKUP_SUCCEEDED("3ds:verify:lookup:succeeded"),
    LOOKUP_FAILED("3ds:verify:lookup:failed"),
    CHALLENGE_REQUIRED("3ds:verify:lookup:challenge-required"),

    // Network Connection Lost
    NETWORK_CONNECTION_LOST("3ds:verify:lookup:network-connection:failed"),

    // Challenge Events
    CHALLENGE_SUCCEEDED("3ds:verify:challenge.succeeded"),
    CHALLENGE_FAILED("3ds:verify:challenge.failed"),

    // JWT Events
    JWT_AUTH_SUCCEEDED("3ds:verify:authenticate-jwt:succeeded"),
    JWT_AUTH_FAILED("3ds:verify:authenticate-jwt:failed");

    private final String event;

    ThreeDSecureAnalytics(String event) {
        this.event = event;
    }

    String getEvent() {
        return event;
    }
}

