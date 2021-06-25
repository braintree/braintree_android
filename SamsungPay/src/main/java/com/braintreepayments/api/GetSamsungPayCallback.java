package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.samsung.android.sdk.samsungpay.v2.SamsungPay;

public interface GetSamsungPayCallback {
    void onResult(@Nullable SamsungPay result, @Nullable Exception error);
}
