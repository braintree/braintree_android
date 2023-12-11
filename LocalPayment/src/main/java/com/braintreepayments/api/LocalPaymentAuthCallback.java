package com.braintreepayments.api;

/**
 * Callback for receiving result of
 * {@link LocalPaymentClient#createPaymentAuthRequest(LocalPaymentRequest, LocalPaymentInternalAuthRequestCallback)}.
 */
public interface LocalPaymentAuthCallback {

    /**
     * @param paymentAuthRequest a request used to launch the PayPal web authentication flow
     */
    void onPayPalPaymentAuthRequest(LocalPaymentAuthRequest paymentAuthRequest);
}
