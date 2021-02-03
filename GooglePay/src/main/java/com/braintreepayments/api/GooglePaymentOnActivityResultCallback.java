package com.braintreepayments.api;

public interface GooglePaymentOnActivityResultCallback {
    void onResult(PaymentMethodNonce paymentMethodNonce, Exception error);
}
