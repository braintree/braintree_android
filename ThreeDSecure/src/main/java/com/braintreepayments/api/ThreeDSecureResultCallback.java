package com.braintreepayments.api;

import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

/**
 * Callback for receiving result of
 * {@link ThreeDSecureClient#performLookup(FragmentActivity, ThreeDSecureRequest, ThreeDSecureResultCallback)},
 * {@link ThreeDSecureClient#initiateChallengeWithLookup(FragmentActivity, ThreeDSecureRequest, ThreeDSecureResult, ThreeDSecureResultCallback)},
 * {@link ThreeDSecureClient#onBrowserSwitchResult(BrowserSwitchResult, ThreeDSecureResultCallback)}, and
 * {@link ThreeDSecureClient#onActivityResult(int, Intent, ThreeDSecureResultCallback)}.
 */
public interface ThreeDSecureResultCallback {

    /**
     * @param threeDSecureResult {@link ThreeDSecureResult}
     * @param error an exception that occurred while processing a 3D Secure result
     */
    void onResult(@Nullable ThreeDSecureResult threeDSecureResult, @Nullable Exception error);
}
