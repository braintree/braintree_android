package com.braintreepayments.api;

public interface BraintreeAuthCallback {
    void onAuthResult(String authString, Exception error);
}
