package com.braintreepayments.api;

import com.braintreepayments.api.models.PaymentMethodNonce;

public interface ThreeDSecureInitializeChallengeCallback {
    void onResult(PaymentMethodNonce nonce, Exception error);
}
