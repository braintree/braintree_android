package com.braintreepayments.api;

import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

/**
 * Callback for receiving result of
 * {@link ThreeDSecureClient#performVerification(FragmentActivity, ThreeDSecureRequest, ThreeDSecureResultCallback)},
 * {@link ThreeDSecureClient#continuePerformVerification(FragmentActivity, ThreeDSecureRequest, ThreeDSecureResult)},
 * {@link ThreeDSecureClient#onBrowserSwitchResult(BrowserSwitchResult)}, and
 * {@link ThreeDSecureClient#onActivityResult(int, Intent, ThreeDSecureResultCallback)}.
 */
public interface ThreeDSecureResultCallback {

    /**
     * @param threeDSecureResult {@link ThreeDSecureResult}
     * @param error an exception that occurred while processing a 3D Secure result
     */
    void onResult(@Nullable ThreeDSecureResult threeDSecureResult, @Nullable Exception error);
}
