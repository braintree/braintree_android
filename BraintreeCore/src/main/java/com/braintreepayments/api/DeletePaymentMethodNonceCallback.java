package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface DeletePaymentMethodNonceCallback {
    void onResult(@Nullable PaymentMethodNonce deletedNonce, @Nullable Exception error);
}
