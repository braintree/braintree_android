package com.braintreepayments.api;

/**
 * Callback used to instantiate a {@link GooglePayLauncher} to handle Activity results from the
 * Google Pay payment flow
 */
public interface GooglePayLauncherCallback {

    void onResult(GooglePayPaymentAuthResult googlePayPaymentAuthResult);
}
