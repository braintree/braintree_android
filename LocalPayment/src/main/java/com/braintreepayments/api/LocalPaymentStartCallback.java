package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link LocalPaymentClient#startPayment(LocalPaymentRequest, LocalPaymentStartCallback)}.
 */
public interface LocalPaymentStartCallback {

    /**
     * @param localPaymentResult {@link LocalPaymentResult}
     * @param error an exception that occurred while initiating a Local Payment
     */
    void onResult(@Nullable LocalPaymentResult localPaymentResult, @Nullable Exception error);
}
