package com.braintreepayments.api;

public interface VisaCheckoutTokenizeCallback {
    void onResult(PaymentMethodNonce paymentMethodNonce, Exception error);
}
