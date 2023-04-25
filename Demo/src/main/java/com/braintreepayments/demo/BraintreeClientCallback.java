package com.braintreepayments.demo;

import androidx.annotation.NonNull;

import com.braintreepayments.api.BraintreeClient;

// NEXT MAJOR VERSION: remove
public interface BraintreeClientCallback {
    void onResult(@NonNull BraintreeClient braintreeClient);
}
