package com.braintreepayments.api;


import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link GooglePayClient#tokenize(GooglePayResult, GooglePayOnActivityResultCallback)}.
 */
public interface GooglePayOnActivityResultCallback {

    /**
     * @param paymentMethodNonce {@link PaymentMethodNonce}
     * @param error              an exception that occurred while processing Google Pay activity
     *                           result
     */
    void onResult(@Nullable PaymentMethodNonce paymentMethodNonce, @Nullable Exception error);
}
