package com.braintreepayments.api.paypal

import com.braintreepayments.api.BrowserSwitchPendingRequest
import org.json.JSONException

/**
 * A pending request for the PayPal web-based authentication flow created by invoking
 * [PayPalLauncher.launch]. This pending request should be stored locally within the app or
 * on-device and used to deliver a result of the browser flow in
 * [PayPalLauncher.handleReturnToAppFromBrowser]
 */
sealed class PayPalPendingRequest {

    /**
     * A pending request was successfully started. This [PayPalPendingRequest.Started] should be
     * stored and passed to [PayPalLauncher.handleReturnToAppFromBrowser]
     */
    class Started(internal val request: BrowserSwitchPendingRequest.Started) : PayPalPendingRequest() {

        /**
         * Convenience constructor to create a [PayPalPendingRequest.Started] from your stored
         * [String] obtained from [PayPalPendingRequest.Started.toJsonString]
         * @throws [JSONException] if the [jsonString] is invalid
         */
        @Throws(JSONException::class)
        constructor(jsonString: String) : this(BrowserSwitchPendingRequest.Started(jsonString))

        /**
         * Convenience method to return [PayPalPendingRequest.Started] in [String] format to be
         * persisted in storage
         */
        fun toJsonString(): String {
            return request.toJsonString()
        }
    }

    /**
     * An error occurred launching the PayPal browser flow. See [error] for details.
     */
    class Failure(val error: Exception) : PayPalPendingRequest()
}
