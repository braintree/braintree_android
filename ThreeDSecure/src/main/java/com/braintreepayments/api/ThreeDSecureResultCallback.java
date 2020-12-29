package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.braintreepayments.api.models.PaymentMethodNonce;

public interface ThreeDSecureResultCallback {
    void onResult(@Nullable PaymentMethodNonce paymentMethodNonce, @Nullable Exception error);
}
