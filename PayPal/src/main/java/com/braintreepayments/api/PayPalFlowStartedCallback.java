package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface PayPalFlowStartedCallback {
    void onResult(@Nullable Exception error);
}
