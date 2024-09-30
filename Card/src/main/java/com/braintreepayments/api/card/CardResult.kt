package com.braintreepayments.api.card

/**
 * Result of tokenizing a [Card]
 */
sealed class CardResult {

    /**
     * The card tokenization completed successfully. This [nonce] should be sent to your server.
     */
    class Success internal constructor(val nonce: CardNonce) : CardResult()

    /**
     * There was an [error] during card tokenization.
     */
    class Failure internal constructor(val error: Exception) : CardResult()
}
