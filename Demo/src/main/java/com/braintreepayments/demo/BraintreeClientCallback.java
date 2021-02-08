package com.braintreepayments.demo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.braintreepayments.api.BraintreeClient;

public interface BraintreeClientCallback {
    void onResult(@NonNull BraintreeClient braintreeClient);
}
