package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link ThreeDSecureClient#createPaymentAuthRequest(android.content.Context, ThreeDSecureRequest, ThreeDSecureResultCallback)}
 * and {@link ThreeDSecureClient#tokenize(ThreeDSecurePaymentAuthResult, ThreeDSecureResultCallback)}.
 */
// TODO: Refactor into two callbacks when result type are separated
public interface ThreeDSecureResultCallback {

    /**
     * @param threeDSecureResult {@link ThreeDSecureResult}
     * @param error              an exception that occurred while processing a 3D Secure result
     */
    void onResult(@Nullable ThreeDSecureResult threeDSecureResult, @Nullable Exception error);
}
