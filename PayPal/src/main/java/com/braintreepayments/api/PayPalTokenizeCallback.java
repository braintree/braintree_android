package com.braintreepayments.api;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link PayPalClient#tokenize(PayPalPaymentAuthResult, PayPalTokenizeCallback)}.
 */
public interface PayPalTokenizeCallback {

    /**
     *
     * @param payPalResult a success, failure, or cancel result from the PayPal flow
     */
    void onResult(@NonNull PayPalResult payPalResult);
}
