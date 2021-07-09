package com.braintreepayments.api;

import androidx.annotation.Nullable;

interface GetSamsungPayStatusCallback {
    void onResult(@Nullable Integer result, @Nullable Exception error);
}
