package com.braintreepayments.api;

public interface BraintreeAuthProvider {
    void getAuthorization(BraintreeAuthCallback callback);
}
