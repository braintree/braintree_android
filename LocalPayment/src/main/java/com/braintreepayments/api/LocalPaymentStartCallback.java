package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link LocalPaymentClient#startPayment(LocalPaymentRequest, LocalPaymentStartCallback)}.
 */
public interface LocalPaymentStartCallback {

    /**
     * @param transaction {@link LocalPaymentTransaction}
     * @param error an exception that occurred while initiating a Local Payment transaction
     */
    void onResult(@Nullable LocalPaymentTransaction transaction, @Nullable Exception error);
}
