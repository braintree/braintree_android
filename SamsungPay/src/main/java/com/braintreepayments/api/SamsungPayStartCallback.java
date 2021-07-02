package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface SamsungPayStartCallback {
    void onResult(@Nullable SamsungPayNonce samsungPayNonce, @Nullable Exception error);
}
