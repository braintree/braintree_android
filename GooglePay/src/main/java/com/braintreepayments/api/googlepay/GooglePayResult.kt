package com.braintreepayments.api.googlepay

import com.braintreepayments.api.PaymentMethodNonce

/**
 * Result of tokenizing a Google Pay payment method
 */
sealed class GooglePayResult {

    /**
     * The Google Pay flow completed successfully. This [nonce] should be sent to your server.
     */
    class Success(val nonce: PaymentMethodNonce) : GooglePayResult()

    /**
     * There was an [error] in the Google Pay flow.
     */
    class Failure(val error: Exception) : GooglePayResult()

    /**
     * The user canceled the Google Pay flow.
     */
    object Cancel : GooglePayResult()
}
