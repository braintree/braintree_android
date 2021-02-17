package com.braintreepayments.api;

public interface GooglePayRequestPaymentCallback {

    void onResult(boolean paymentRequested, Exception error);
}
