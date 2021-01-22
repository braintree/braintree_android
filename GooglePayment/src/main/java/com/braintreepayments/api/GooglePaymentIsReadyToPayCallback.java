package com.braintreepayments.api;

public interface GooglePaymentIsReadyToPayCallback {

    void onResult(Boolean isReadyToPay, Exception error);
}
