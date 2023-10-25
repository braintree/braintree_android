package com.braintreepayments.api;

public class Success extends PayPalResult {
    private final PayPalAccountNonce nonce;
    public Success(PayPalAccountNonce nonce) {
        this.nonce = nonce;
    }

    public PayPalAccountNonce getNonce() {
        return nonce;
    }
}
