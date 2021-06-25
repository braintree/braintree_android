package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface GetSamsungPayInternalClientCallback {
    void onResult(@Nullable SamsungPayInternalClient internalClient, @Nullable Exception error);
}
