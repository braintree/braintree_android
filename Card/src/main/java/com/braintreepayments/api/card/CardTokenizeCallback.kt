package com.braintreepayments.api.card

/**
 * Callback for receiving result of [CardClient.tokenize].
 */
fun interface CardTokenizeCallback {

    /**
     * @param cardResult a [CardResult] containing a [CardNonce] or [Exception]
     */
    fun onCardResult(cardResult: CardResult?)
}
