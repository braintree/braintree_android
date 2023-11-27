package com.braintreepayments.api;


import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link ThreeDSecureClient#createPaymentAuthRequest(android.content.Context, ThreeDSecureRequest, ThreeDSecureResultCallback)},

 */
// TODO: Split into separate callbacks for internal and public methods and for
//  createPaymentAuthRequest and tokenize methods
public interface ThreeDSecureResultCallback {

    /**
     * @param threeDSecureResult {@link ThreeDSecureResult}
     * @param error              an exception that occurred while processing a 3D Secure result
     */
    void onResult(@Nullable ThreeDSecureResult threeDSecureResult, @Nullable Exception error);
}
