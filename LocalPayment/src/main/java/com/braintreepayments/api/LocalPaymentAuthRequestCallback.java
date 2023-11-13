package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link LocalPaymentClient#createPaymentAuthRequest(LocalPaymentRequest, LocalPaymentAuthRequestCallback)}.
 */
public interface LocalPaymentAuthRequestCallback {

    /**
     * @param localPaymentAuthRequest {@link LocalPaymentAuthRequest}
     * @param error an exception that occurred while initiating a Local Payment
     */
    void onResult(@Nullable LocalPaymentAuthRequest localPaymentAuthRequest, @Nullable Exception error);
}
