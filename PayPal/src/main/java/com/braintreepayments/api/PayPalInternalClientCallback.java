package com.braintreepayments.api;

public interface PayPalInternalClientCallback {
    void onResult(PayPalResponse payPalResponse, Exception error);
}
