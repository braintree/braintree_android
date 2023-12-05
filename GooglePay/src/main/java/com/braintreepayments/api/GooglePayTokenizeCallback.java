package com.braintreepayments.api;


import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link GooglePayClient#tokenize(GooglePayPaymentAuthResult, GooglePayTokenizeCallback)}.
 */
public interface GooglePayTokenizeCallback {

    /**
     * @param googlePayResult {@link PaymentMethodNonce}
     */
    void onResult(@Nullable GooglePayResult googlePayResult);
}
