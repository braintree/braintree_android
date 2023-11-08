package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback to handle result from
 * {@link GooglePayClient#createPaymentAuthRequest(GooglePayRequest, GooglePayPaymentAuthRequestCallback)}
 */
public interface GooglePayPaymentAuthRequestCallback {

    void onGooglePayIntentData(@Nullable GooglePayPaymentAuthRequest googlePayPaymentAuthRequest,
                               @Nullable Exception error);
}
