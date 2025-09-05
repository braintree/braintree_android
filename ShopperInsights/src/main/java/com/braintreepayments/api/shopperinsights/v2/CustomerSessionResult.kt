package com.braintreepayments.api.shopperinsights.v2

/**
 * Represents the result of a customer session creation.
 */
sealed class CustomerSessionResult {

    /**
     * Indicates a successful customer session creation.
     *
     * @property sessionId ID of the session created
     */
    class Success internal constructor(val sessionId: String) : CustomerSessionResult()

    /**
     * Indicates a failure during customer session creation.
     *
     * @property error The exception that caused the failure.
     */
    class Failure internal constructor(val error: Exception) : CustomerSessionResult()
}
