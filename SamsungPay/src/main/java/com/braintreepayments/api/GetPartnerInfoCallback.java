package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.samsung.android.sdk.samsungpay.v2.PartnerInfo;

public interface GetPartnerInfoCallback {
    void onResult(@Nullable PartnerInfo partnerInfo, @Nullable Exception error);
}
