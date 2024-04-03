package com.braintreepayments.api.threedsecure

/**
 * Result of upgrading a [PaymentMethodNonce] with 3D Secure Authentication
 */
sealed class ThreeDSecureResult {

    /**
     * The 3DS flow completed successfully. This [nonce] should be sent to your server.
     */
    class Success(val nonce: ThreeDSecureNonce) :
        ThreeDSecureResult()

    /**
     * There was an [error] in the 3DS authentication flow. Optionally may return a [nonce] if
     * the authentication was not successful but the [nonce] from the [ThreeDSecureLookup] can be
     * transacted with.
     */
    class Failure(val error: Exception, val nonce: ThreeDSecureNonce?) : ThreeDSecureResult()

    /**
     * The user canceled the 3DS authentication flow.
     */
    object Cancel : ThreeDSecureResult()
}
