package com.braintreepayments.api.venmo

/**
 * A request used to launch the Venmo app for continuation of the Venmo payment flow.
 */
sealed class VenmoPaymentAuthRequest {

    // TODO: make requestParams internal when VenmoClientUnitTest is converted to Kotlin
    /**
     * The request was successfully created and is ready to be launched by [VenmoLauncher]
     * @param requestParams this parameter is intended for internal use only. It is not covered by
     * semantic versioning and may be changed or removed at any time.
     */
    class ReadyToLaunch internal constructor(
        val requestParams: VenmoPaymentAuthRequestParams
    ) : VenmoPaymentAuthRequest()

    /**
     * There was an [error] creating the request
     */
    class Failure internal constructor(val error: Exception) : VenmoPaymentAuthRequest()
}
