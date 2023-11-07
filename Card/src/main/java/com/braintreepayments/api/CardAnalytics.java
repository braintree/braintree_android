package com.braintreepayments.api;

enum CardAnalytics {

    CARD_TOKENIZE_STARTED("card:tokenize:started"),
    CARD_TOKENIZE_FAILED("card:tokenize:failed"),
    CARD_TOKENIZE_SUCCEEDED("card:tokenize:succeeded"),
    CARD_TOKENIZE_NETWORK_CONNECTION_LOST("card:tokenize:network-connection:failed");

    private final String event;

    CardAnalytics(String event) {
        this.event = event;
    }

    String getEvent() {
        return event;
    }
}
