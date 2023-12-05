package com.braintreepayments.api

/**
 * Result of tokenizing a [Card]
 */
sealed class CardResult {

    /**
     * The card tokenization completed successfully. This [nonce] should be sent to your server.
     */
    class Success(val nonce: CardNonce) : CardResult()

    /**
     * There was an [error] during card tokenization.
     */
    class Failure(val error: Exception) : CardResult()

}
