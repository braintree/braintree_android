package com.braintreepayments.api;

import androidx.annotation.Nullable;

public interface ThreeDSecureResultCallback {
    void onResult(@Nullable PaymentMethodNonce paymentMethodNonce, @Nullable Exception error);
}
