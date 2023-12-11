package com.braintreepayments.api;

/**
 * Callback for receiving result of
 * {@link LocalPaymentClient#createPaymentAuthRequest(LocalPaymentRequest, LocalPaymentAuthCallback)}.
 */
public interface LocalPaymentAuthCallback {

    /**
     * @param paymentAuthRequest a request used to launch the PayPal web authentication flow
     */
    void onLocalPaymentAuthRequest(LocalPaymentAuthRequest paymentAuthRequest);
}
