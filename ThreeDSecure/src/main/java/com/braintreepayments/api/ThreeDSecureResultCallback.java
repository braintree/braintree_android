package com.braintreepayments.api;

import android.content.Intent;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link ThreeDSecureClient#performVerification(android.content.Context, ThreeDSecureRequest, ThreeDSecureResultCallback)},
 * {@link ThreeDSecureClient#continuePerformVerification(ThreeDSecureResult, ThreeDSecureResultCallback)},
 * {@link ThreeDSecureClient#onBrowserSwitchResult(BrowserSwitchResult,
 * ThreeDSecureResultCallback)}, and
 * {@link ThreeDSecureClient#onActivityResult(int, Intent, ThreeDSecureResultCallback)}.
 */
public interface ThreeDSecureResultCallback {

    /**
     * @param threeDSecureResult {@link ThreeDSecureResult}
     * @param error              an exception that occurred while processing a 3D Secure result
     */
    void onResult(@Nullable ThreeDSecureResult threeDSecureResult, @Nullable Exception error);
}
