package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link LocalPaymentClient#tokenize(Context, LocalPaymentAuthResult, LocalPaymentTokenizeCallback)}.
 */
public interface LocalPaymentTokenizeCallback {

    /**
     * @param localPaymentNonce {@link LocalPaymentNonce}
     * @param error an exception that occurred while processing Local Payment result
     */
    void onResult(@Nullable LocalPaymentNonce localPaymentNonce, @Nullable Exception error);
}
