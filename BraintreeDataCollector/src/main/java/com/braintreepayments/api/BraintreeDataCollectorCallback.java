package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface BraintreeDataCollectorCallback {
    void onResult(@Nullable String deviceData, @Nullable Exception error);
}
