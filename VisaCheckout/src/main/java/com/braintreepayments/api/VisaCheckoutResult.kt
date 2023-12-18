package com.braintreepayments.api

/**
 * Result of tokenizing a Visa Checkout account
 */
sealed class VisaCheckoutResult {

    /**
     * The Visa Checkout flow completed successfully. This [nonce] should be sent to your server.
     */
    class Success(val nonce: VisaCheckoutNonce) : VisaCheckoutResult()

    /**
     * There was an [error] in the Visa Checkout flow.
     */
    class Failure(val error: Exception) : VisaCheckoutResult()
}
