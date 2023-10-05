package com.braintreepayments.api;

public class VenmoResult {

    private VenmoAccountNonce venmoAccountNonce;
    private Exception error;
    VenmoResult(VenmoAccountNonce venmoAccountNonce) {
        this.venmoAccountNonce = venmoAccountNonce;
    }

    VenmoResult(Exception venmoError) {
        this.error = venmoError;
    }

    public VenmoAccountNonce getVenmoAccountNonce() {
        return venmoAccountNonce;
    }

    public Exception getError() {
        return error;
    }
}
