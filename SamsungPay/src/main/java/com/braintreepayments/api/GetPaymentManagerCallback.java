package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.samsung.android.sdk.samsungpay.v2.payment.PaymentManager;

public interface GetPaymentManagerCallback {
    void onResult(@Nullable PaymentManager result, @Nullable Exception error);
}
