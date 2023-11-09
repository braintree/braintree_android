package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link ThreeDSecureClient#createPaymentAuthRequest(android.content.Context, ThreeDSecureRequest, ThreeDSecurePaymentAuthRequestCallback)}.
 */
public interface ThreeDSecurePaymentAuthRequestCallback {

    /**
     * @param paymentAuthRequest {@link ThreeDSecurePaymentAuthRequest}
     * @param error              an exception that occurred while processing a 3D Secure result
     */
    void onResult(@Nullable ThreeDSecurePaymentAuthRequest paymentAuthRequest, @Nullable Exception error);
}
