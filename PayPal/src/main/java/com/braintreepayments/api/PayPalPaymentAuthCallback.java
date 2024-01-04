package com.braintreepayments.api;

/**
 * Callback for receiving result of
 * {@link PayPalClient#createPaymentAuthRequest(android.content.Context, PayPalRequest, PayPalPaymentAuthCallback)}.
 */
public interface PayPalPaymentAuthCallback {

    /**
     * @param paymentAuthRequest a request used to launch the PayPal web authentication flow
     */
    void onPayPalPaymentAuthRequest(PayPalPaymentAuthRequest paymentAuthRequest);
}
