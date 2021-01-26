package com.braintreepayments.api;

import androidx.annotation.Nullable;

interface ThreeDSecureInitializeChallengeCallback {
    void onResult(@Nullable PaymentMethodNonce nonce, @Nullable Exception error);
}
