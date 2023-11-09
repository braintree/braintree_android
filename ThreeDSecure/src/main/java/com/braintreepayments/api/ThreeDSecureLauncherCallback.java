package com.braintreepayments.api;

import androidx.annotation.NonNull;

/**
 * Callback for receiving the results via {@link ThreeDSecureLauncher}
 */
public interface ThreeDSecureLauncherCallback {

    void onResult(@NonNull ThreeDSecurePaymentAuthResult paymentAuthResult);
}
