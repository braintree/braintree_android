package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.samsung.android.sdk.samsungpay.v2.SpaySdk;

import java.util.List;

public interface GetAcceptedCardBrandsCallback {

    void onResult(@Nullable List<SpaySdk.Brand> acceptedCardBrands, @Nullable Exception error);
}
