package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface StartSamsungPayCallback {
    void onResult(@Nullable SamsungPayNonce samsungPayNonce, @Nullable Exception error);
}
