package com.braintreepayments.api

import org.json.JSONException

/**
 * A pending request for the local payment web-based authentication flow created by invoking
 * [LocalPaymentLauncher.launch]. This pending request should be stored locally within the app or
 * on-device and used to deliver a result of the browser flow in
 * [LocalPaymentLauncher.handleReturnToAppFromBrowser]
 */
sealed class LocalPaymentPendingRequest {

    /**
     * A pending request was successfully started. This [LocalPaymentPendingRequest.Started] should be
     * stored and passed to [LocalPaymentLauncher.handleReturnToAppFromBrowser]
     */
    class Started(internal val request: BrowserSwitchPendingRequest.Started) : LocalPaymentPendingRequest() {

        /**
         * Convenience constructor to create a [LocalPaymentPendingRequest.Started] from your stored
         * [String] obtained from [LocalPaymentPendingRequest.Started.toJsonString]
         * @throws [JSONException] if the [jsonString] is invalid
         */
        @Throws(JSONException::class)
        constructor(jsonString: String) : this(BrowserSwitchPendingRequest.Started(jsonString))

        /**
         * Convenience method to return [LocalPaymentPendingRequest.Started] in [String] format to be
         * persisted in storage
         */
        fun toJsonString(): String {
            return request.toJsonString()
        }
    }

    /**
     * An error occurred launching the local payment browser flow. See [error] for details.
     */
    class Failure(val error: Exception) : LocalPaymentPendingRequest()
}
