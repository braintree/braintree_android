package com.braintreepayments.api.venmo

/**
 * A pending request for the Venmo authentication flow created by invoking
 * [VenmoLauncher.launch]. This pending request should be stored locally within the app or
 * on-device and used to deliver a result of the browser flow in
 * [VenmoLauncher.handleReturnToApp]
 */
sealed class VenmoPendingRequest {

    /**
     * A pending request was successfully started.
     *
     * @property pendingRequestString - This String should be stored and passed to
     * [VenmoLauncher.handleReturnToApp].
     */
    class Started(val pendingRequestString: String) : VenmoPendingRequest()

    /**
     * An error occurred launching the Venmo flow. See [error] for details.
     */
    class Failure internal constructor(val error: Exception) : VenmoPendingRequest()
}
