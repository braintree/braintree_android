package com.braintreepayments.api;

public class Failure extends PayPalResult {

    private final Exception error;

    public Failure(Exception error) {
        this.error = error;
    }

    public Exception getError() {
        return error;
    }

}
