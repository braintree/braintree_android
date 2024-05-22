package com.braintreepayments.api.venmo

/**
 * Result of tokenizing a Venmo account
 */
sealed class VenmoResult {

    /**
     * The Venmo flow completed successfully. This [nonce] should be sent to your server.
     */
    class Success(val nonce: VenmoAccountNonce) : VenmoResult()

    /**
     * There was an [error] in the Venmo payment flow.
     */
    class Failure(val error: Exception) : VenmoResult()

    /**
     * The user canceled the Venmo payment flow.
     */
    object Cancel : VenmoResult()
}
