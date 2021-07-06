package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.samsung.android.sdk.samsungpay.v2.payment.CustomSheetPaymentInfo;

public interface BuildCustomSheetPaymentInfoCallback {
    void onResult(@Nullable CustomSheetPaymentInfo.Builder builder, @Nullable Exception error);
}
