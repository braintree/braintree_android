package com.braintreepayments.api.localpayment

/**
 * Result of tokenizing a local payment method
 */
sealed class LocalPaymentResult {

    /**
     * The local payment flow completed successfully. This [nonce] should be sent to your server.
     */
    class Success internal constructor(val nonce: LocalPaymentNonce) : LocalPaymentResult()

    /**
     * There was an [error] in the local payment flow.
     */
    class Failure internal constructor(val error: Exception) : LocalPaymentResult()

    /**
     * The user canceled the local payment flow.
     */
    data object Cancel : LocalPaymentResult()
}
