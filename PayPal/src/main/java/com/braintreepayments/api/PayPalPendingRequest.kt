package com.braintreepayments.api

import org.json.JSONObject

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
    class Started(val request: PayPalBrowserSwitchRequest) : PayPalPendingRequest() {

        /**
         * Convenience constructor to create a [PayPalPendingRequest.Started] from your stored
         * [String] from [PayPalPendingRequest.Started.toJsonString]
         */
        constructor(jsonString: String) : this(PayPalBrowserSwitchRequest(
            BrowserSwitchPendingRequest.Started(BrowserSwitchRequest.fromJson(
            JSONObject(jsonString).getString(
                "browserSwitchRequest"
            )))))

        /**
         * Convenience method to return [PayPalPendingRequest.Started] in [String] format to be
         * persisted in storage
         */
        fun toJsonString(): String {
            val json = JSONObject()
            json.put("browserSwitchRequest",
                request.browserSwitchPendingRequest.browserSwitchRequest.toJson())
            return json.toString()
        }
    }

    /**
     * An error occurred launching the PayPal browser flow. See [error] for details.
     */
    class Failure(val error: Exception) : PayPalPendingRequest()
}
