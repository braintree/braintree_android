package com.braintreepayments.api;

import androidx.annotation.Nullable;

/**
 * Callback for receiving result of
 * {@link UnionPayClient#tokenize(UnionPayCardBuilder, UnionPayTokenizeCallback)}.
 */
public interface UnionPayTokenizeCallback {

    /**
     * @param cardNonce {@link CardNonce}
     * @param error an exception that occurred while tokenizing a Union Pay card
     */
    void onResult(@Nullable CardNonce cardNonce, @Nullable Exception error);
}
