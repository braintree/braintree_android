package com.braintreepayments.api;

import android.content.Context;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of {@link CardClient#tokenize(Context, Card, CardTokenizeCallback)}.
 */
public interface CardTokenizeCallback {

    /**
     * @param cardNonce {@link CardNonce}
     * @param error an exception that occurred while tokenizing card
     */
    void onResult(@Nullable CardNonce cardNonce, @Nullable Exception error);
}
