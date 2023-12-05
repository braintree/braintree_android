package com.braintreepayments.api;

/**
 * Callback for receiving result of {@link CardClient#tokenize(Card, CardTokenizeCallback)}.
 */
public interface CardTokenizeCallback {

    /**
     * @param cardResult a {@link CardResult} containing a {@link CardNonce} or {@link Exception}
     */
    void onCardResult(CardResult cardResult);
}
