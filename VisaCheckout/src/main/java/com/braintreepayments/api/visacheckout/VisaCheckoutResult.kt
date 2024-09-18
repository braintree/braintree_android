package com.braintreepayments.api.visacheckout

/**
 * Result of tokenizing a Visa Checkout account
 */
sealed class VisaCheckoutResult {

    /**
     * The Visa Checkout flow completed successfully. This [nonce] should be sent to your server.
     */
    class Success internal constructor(val nonce: VisaCheckoutNonce) : VisaCheckoutResult()

    /**
     * There was an [error] in the Visa Checkout flow.
     */
    class Failure internal constructor(val error: Exception) : VisaCheckoutResult()
}
