package com.braintreepayments.api

import org.json.JSONException

/**
 * A pending request for the Venmo authentication flow created by invoking
 * [VenmoLauncher.launch]. This pending request should be stored locally within the app or
 * on-device and used to deliver a result of the browser flow in
 * [VenmoLauncher.handleReturnToApp]
 */
sealed class VenmoPendingRequest {

    /**
     * A pending request was successfully started. This [VenmoPendingRequest.Started] should be
     * stored and passed to [VenmoLauncher.handleReturnToApp]
     */
    class Started(internal val request: BrowserSwitchPendingRequest.Started) : VenmoPendingRequest() {

        /**
         * Convenience constructor to create a [VenmoPendingRequest.Started] from your stored
         * [String] obtained from [VenmoPendingRequest.Started.toJsonString]
         * @throws [JSONException] if the [jsonString] is invalid
         */
        @Throws(JSONException::class)
        constructor(jsonString: String) : this(BrowserSwitchPendingRequest.Started(jsonString))

        /**
         * Convenience method to return [VenmoPendingRequest.Started] in [String] format to be
         * persisted in storage
         */
        fun toJsonString(): String {
            return request.toJsonString()
        }
    }

    /**
     * An error occurred launching the Venmo flow. See [error] for details.
     */
    class Failure(val error: Exception) : VenmoPendingRequest()
}
