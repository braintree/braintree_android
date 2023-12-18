package com.braintreepayments.api;

/**
 * Callback to handle result from
 * {@link GooglePayClient#createPaymentAuthRequest(GooglePayRequest, GooglePayPaymentAuthRequestCallback)}
 */
public interface GooglePayPaymentAuthRequestCallback {

    void onGooglePayPaymentAuthRequest(GooglePayPaymentAuthRequest paymentAuthRequest);
}
