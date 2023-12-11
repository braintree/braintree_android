package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.NonNull;

/**
 * Callback for receiving result of
 * {@link LocalPaymentClient#tokenize(Context, LocalPaymentAuthResult, LocalPaymentTokenizeCallback)}.
 */
public interface LocalPaymentTokenizeCallback {

    /**
     *
     * @param localPaymentResult a success, failure, or cancel result from the local payment flow
     */
    void onLocalPaymentResult(@NonNull LocalPaymentResult localPaymentResult);
}
