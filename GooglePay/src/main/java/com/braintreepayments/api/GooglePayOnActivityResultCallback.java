package com.braintreepayments.api;

public interface GooglePayOnActivityResultCallback {
    void onResult(PaymentMethodNonce paymentMethodNonce, Exception error);
}
