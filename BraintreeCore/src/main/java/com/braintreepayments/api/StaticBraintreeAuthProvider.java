package com.braintreepayments.api;

class StaticBraintreeAuthProvider implements BraintreeAuthProvider {

    private final String authString;

    public StaticBraintreeAuthProvider(String authString) {
        this.authString = authString;
    }

    @Override
    public void getAuthorization(BraintreeAuthCallback callback) {
        callback.onAuthResult(authString, null);
    }
}
