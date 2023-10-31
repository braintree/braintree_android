package com.braintreepayments.api;

public interface TestCallback {

    void onResult(PayPalAccountNonce nonce);

    void onError(Exception error);

    void onCancel();
}
