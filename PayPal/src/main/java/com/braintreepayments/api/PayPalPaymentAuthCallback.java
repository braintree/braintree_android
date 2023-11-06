package com.braintreepayments.api;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

/**
 * Callback for receiving result of
 * {@link PayPalClient#createPaymentAuthRequest(FragmentActivity, PayPalRequest, PayPalPaymentAuthCallback)}.
 */
public interface PayPalPaymentAuthCallback {

    /**
     * @param payPalPaymentAuthRequest a request used to launch the PayPal web authentication flow
     * @param error          an exception that occurred while initiating a PayPal transaction
     */
    void onResult(PayPalPaymentAuthRequest payPalPaymentAuthRequest, @Nullable Exception error);
}
