package com.braintreepayments.api;

enum AmericanExpressAnalytics {

    REWARDS_BALANCE_STARTED("amex:rewards-balance:started"),
    REWARDS_BALANCE_FAILED("amex:rewards-balance:failed"),
    REWARDS_BALANCE_SUCCEEDED("amex:rewards-balance:succeeded");

    private final String event;

    AmericanExpressAnalytics(String event) {
        this.event = event;
    }

    String getEvent() {
        return event;
    }
}


