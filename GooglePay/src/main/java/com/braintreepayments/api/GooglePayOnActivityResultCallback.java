package com.braintreepayments.api;

import android.content.Intent;

import androidx.fragment.app.FragmentActivity;

/**
 * Callback for receiving result of
 * {@link GooglePayClient#onActivityResult(FragmentActivity, int, Intent, GooglePayOnActivityResultCallback)}.
 */
public interface GooglePayOnActivityResultCallback {

    /**
     * @param paymentMethodNonce {@link PaymentMethodNonce}
     * @param error an exception that occurred while processing Google Pay activity result
     */
    void onResult(PaymentMethodNonce paymentMethodNonce, Exception error);
}
