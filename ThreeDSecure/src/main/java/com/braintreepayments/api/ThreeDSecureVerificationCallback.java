package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.braintreepayments.api.models.PaymentMethodNonce;
import com.braintreepayments.api.models.ThreeDSecureLookup;

public interface ThreeDSecureVerificationCallback {
    void onResult(@Nullable PaymentMethodNonce paymentMethodNonce, @Nullable Exception error);
}
