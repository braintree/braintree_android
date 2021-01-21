package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface DataCollectorCallback {
    void onResult(@Nullable String deviceData, @Nullable Exception error);
}
