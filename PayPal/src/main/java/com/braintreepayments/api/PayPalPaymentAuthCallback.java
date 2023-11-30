package com.braintreepayments.api;

import androidx.fragment.app.FragmentActivity;

/**
 * Callback for receiving result of
 * {@link PayPalClient#createPaymentAuthRequest(FragmentActivity, PayPalRequest, PayPalPaymentAuthCallback)}.
 */
public interface PayPalPaymentAuthCallback {

    /**
     * @param paymentAuthRequest a request used to launch the PayPal web authentication flow
     */
    void onResult(PayPalPaymentAuthRequest paymentAuthRequest);
}
