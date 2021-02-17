package com.braintreepayments.api;

public interface GooglePayIsReadyToPayCallback {

    void onResult(Boolean isReadyToPay, Exception error);
}
