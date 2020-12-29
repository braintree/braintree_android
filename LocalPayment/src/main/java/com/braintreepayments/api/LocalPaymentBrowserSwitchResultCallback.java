package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.braintreepayments.api.models.LocalPaymentResult;

public interface LocalPaymentBrowserSwitchResultCallback {
    void onResult(@Nullable LocalPaymentResult result, @Nullable Exception error);
}
