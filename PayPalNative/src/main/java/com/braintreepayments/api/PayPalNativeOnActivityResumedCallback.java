package com.braintreepayments.api;

public interface PayPalNativeOnActivityResumedCallback {

    void onResult(PayPalAccountNonce payPalAccountNonce, Exception error);
}
