package com.braintreepayments.api.paypal

/**
 * Result of tokenizing a PayPal account
 */
sealed class PayPalResult {

    /**
     * The PayPal flow completed successfully. This [nonce] should be sent to your server.
     */
    class Success internal constructor(val nonce: PayPalAccountNonce) : PayPalResult()

    /**
     * There was an [error] in the PayPal payment flow.
     */
    class Failure internal constructor(val error: Exception) : PayPalResult()

    /**
     * The user canceled the PayPal payment flow.
     */
    data object Cancel : PayPalResult()
}
