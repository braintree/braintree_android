package com.braintreepayments.api;

public class VenmoResult {

    private final String paymentContextId;
    private final String venmoAccountNonce;
    private final String venmoUsername;

    private final Exception error;

    public VenmoResult(String paymentContextId, String venmoAccountNonce, String venmoUsername, Exception error) {
        this.paymentContextId = paymentContextId;
        this.venmoAccountNonce = venmoAccountNonce;
        this.venmoUsername = venmoUsername;
        this.error = error;
    }

    public String getPaymentContextId() {
        return paymentContextId;
    }

    public String getVenmoAccountNonce() {
        return venmoAccountNonce;
    }

    public String getVenmoUsername() {
        return venmoUsername;
    }

    public Exception getError() {
        return error;
    }
}
