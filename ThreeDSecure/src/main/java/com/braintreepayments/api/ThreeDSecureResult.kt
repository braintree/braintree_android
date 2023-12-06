package com.braintreepayments.api

/**
 * Result of upgrading a [PaymentMethodNonce] with 3D Secure Authentication
 */
sealed class ThreeDSecureResult {

    /**
     * The 3DS flow completed successfully. This [nonce] should be sent to your server.
     */
    class Success(val nonce: ThreeDSecureNonce) : ThreeDSecureResult()

    /**
     * There was an [error] in the 3DS authentication flow.
     */
    class Failure(val error: Exception) : ThreeDSecureResult()

    /**
     * The user canceled the 3DS authentication flow.
     */
    object Cancel : ThreeDSecureResult()
}
