package com.braintreepayments.api;

import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

/**
 * Callback for receiving result of
 * {@link ThreeDSecureClient#performVerification(FragmentActivity, ThreeDSecureRequest, ThreeDSecureResultCallback)} and
 * {@link ThreeDSecureClient#continuePerformVerification(FragmentActivity, ThreeDSecureRequest, ThreeDSecureLookup, ThreeDSecureResultCallback)} and
 * {@link ThreeDSecureClient#onBrowserSwitchResult(BrowserSwitchResult, ThreeDSecureResultCallback)} and
 * {@link ThreeDSecureClient#onActivityResult(int, Intent, ThreeDSecureResultCallback)}.
 */
public interface ThreeDSecureResultCallback {

    /**
     * @param paymentMethodNonce {@link PaymentMethodNonce}
     * @param error an exception that occurred while processing a 3D Secure result
     */
    void onResult(@Nullable PaymentMethodNonce paymentMethodNonce, @Nullable Exception error);
}
