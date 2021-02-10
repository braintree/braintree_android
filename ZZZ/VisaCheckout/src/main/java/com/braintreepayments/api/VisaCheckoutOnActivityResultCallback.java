package com.braintreepayments.api;

public interface VisaCheckoutOnActivityResultCallback {
    void onResult(PaymentMethodNonce paymentMethodNonce, Exception error);
}
