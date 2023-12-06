package com.braintreepayments.api;


import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link ThreeDSecureClient#createPaymentAuthRequest(android.content.Context, ThreeDSecureRequest, ThreeDSecurePaymentAuthRequestCallback)},

 */
interface ThreeDSecureResultCallback {

    /**
     * @param threeDSecureParams {@link ThreeDSecureParams}
     * @param error              an exception that occurred while processing a 3D Secure result
     */
    void onResult(@Nullable ThreeDSecureParams threeDSecureParams, @Nullable Exception error);
}
