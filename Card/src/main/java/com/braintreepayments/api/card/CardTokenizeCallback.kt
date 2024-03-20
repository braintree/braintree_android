package com.braintreepayments.api.card

import com.braintreepayments.api.card.CardResult

/**
 * Callback for receiving result of [CardClient.tokenize].
 */
fun interface CardTokenizeCallback {

    /**
     * @param cardResult a [CardResult] containing a [CardNonce] or [Exception]
     */
    fun onCardResult(cardResult: CardResult?)
}
