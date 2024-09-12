package com.braintreepayments.api.threedsecure

/**
 * A request used to launch continuation of the 3D Secure authentication flow.
 */
sealed class ThreeDSecurePaymentAuthRequest {

    // TODO: make requestParams internal when ThreeDSecureClientUnitTest is converted to Kotlin
    /**
     * The request was successfully created and is ready to be launched by [ThreeDSecureLauncher]
     * @param requestParams this parameter is intended for internal use only. It is not covered by
     * semantic versioning and may be changed or removed at any time.
     */
    class ReadyToLaunch(val requestParams: ThreeDSecureParams) :
        ThreeDSecurePaymentAuthRequest()

    /**
     * No additional authentication challenge is required for the [ThreeDSecureNonce], this
     * [nonce] can be sent to your server.
     */
    class LaunchNotRequired(val nonce: ThreeDSecureNonce, val threeDSecureLookup:
    ThreeDSecureLookup
    ) : ThreeDSecurePaymentAuthRequest()

    /**
     * There was an [error] creating the request
     */
    class Failure(val error: Exception) : ThreeDSecurePaymentAuthRequest()
}
