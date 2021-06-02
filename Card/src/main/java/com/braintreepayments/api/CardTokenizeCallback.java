package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of {@link CardClient#tokenize(Card, CardTokenizeCallback)}.
 */
public interface CardTokenizeCallback {

    /**
     * @param cardNonce {@link CardNonce}
     * @param error an exception that occurred while tokenizing card
     */
    void onResult(@Nullable CardNonce cardNonce, @Nullable Exception error);
}
