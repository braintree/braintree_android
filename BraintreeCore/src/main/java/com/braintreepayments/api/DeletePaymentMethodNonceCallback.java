package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.braintreepayments.api.models.PaymentMethodNonce;

public interface DeletePaymentMethodNonceCallback {
    void onResult(@Nullable PaymentMethodNonce deletedNonce, @Nullable Exception error);
}
