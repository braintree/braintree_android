package com.braintreepayments.api;

public interface LocalPaymentListener {
    void onLocalPaymentSuccess(LocalPaymentNonce localPaymentNonce);
    void onLocalPaymentError(Exception error);
}
