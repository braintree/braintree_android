package com.braintreepayments.api;

public interface GooglePaymentRequestPaymentCallback {

    void onResult(boolean paymentRequested, Exception error);
}
