package com.braintreepayments.api.uicomponents.cardfields

import com.braintreepayments.api.card.CardNonce

/**
 * Result of a call to [CardFields.submit]
 */
sealed class CardFieldsResult {

    /**
     * The card tokenization completed successfully. This [nonce] should be sent to your server.
     */
    class Success internal constructor(val nonce: CardNonce) : CardFieldsResult()

    /**
     * There was an [error] during card tokenization.
     */
    class Failure internal constructor(val error: Exception) : CardFieldsResult()
}
