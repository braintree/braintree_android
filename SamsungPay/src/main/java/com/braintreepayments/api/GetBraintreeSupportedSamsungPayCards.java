package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.samsung.android.sdk.samsungpay.v2.SpaySdk;

import java.util.List;

public interface GetBraintreeSupportedSamsungPayCards {
    void onResult(@Nullable List<SpaySdk.Brand> result, @Nullable Exception error);
}
