package com.braintreepayments.api.venmo

/**
 * Result of tokenizing a Venmo account
 */
sealed class VenmoResult {

    /**
     * The Venmo flow completed successfully. This [nonce] should be sent to your server.
     */
    class Success internal constructor(val nonce: VenmoAccountNonce) : VenmoResult()

    /**
     * There was an [error] in the Venmo payment flow.
     */
    class Failure internal constructor(val error: Exception) : VenmoResult()

    /**
     * The user canceled the Venmo payment flow.
     */
    data object Cancel : VenmoResult()
}
