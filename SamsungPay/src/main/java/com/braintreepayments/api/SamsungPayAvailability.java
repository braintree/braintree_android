package com.braintreepayments.api;

public class SamsungPayAvailability {

    private final int status;
    private final int reason;

    SamsungPayAvailability(int status, int reason) {
        this.status = status;
        this.reason = reason;
    }
}
