package com.braintreepayments.api;

public interface VenmoResultCallback {

    void onResult(VenmoAccountNonce venmoAccountNonce, Exception error);
}
