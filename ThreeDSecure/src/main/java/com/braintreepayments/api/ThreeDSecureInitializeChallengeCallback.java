package com.braintreepayments.api;

import androidx.annotation.Nullable;

import com.braintreepayments.api.models.PaymentMethodNonce;

interface ThreeDSecureInitializeChallengeCallback {
    void onResult(@Nullable PaymentMethodNonce nonce, @Nullable Exception error);
}
