package com.braintreepayments.api.uicomponents.cardfields

import com.braintreepayments.api.card.CardNonce

/**
 * Callback for receiving result of [CardFields.submit].
 */
fun interface CardFieldsResultCallback {

    /**
     * @param cardFieldsResult a [CardFieldsResult] containing a [CardNonce] or [Exception]
     */
    fun onCardFieldsResult(cardFieldsResult: CardFieldsResult)
}
