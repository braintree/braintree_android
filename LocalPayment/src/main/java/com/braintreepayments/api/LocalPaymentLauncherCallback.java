package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback for handling results in {@link LocalPaymentLauncher}
 */
public interface LocalPaymentLauncherCallback {

    void onResult(@Nullable LocalPaymentAuthResult localPaymentResult);
}
