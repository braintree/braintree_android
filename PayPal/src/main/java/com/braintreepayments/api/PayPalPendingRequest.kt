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
        fun toJsonString(): String {
            val json = JSONObject()
            json.put("browserSwitchRequest", request.browserSwitchRequest.toJson())
            return json.toString()
        }

        companion object {
            fun fromJsonString(jsonString: String): Started {
                val json = JSONObject(jsonString)
                return Started(
                    PayPalBrowserSwitchRequest(
                        BrowserSwitchRequest.fromJson(
                            json.getString(
                                "browserSwitchRequest"
                            )
                        )
                    )
                )
            }
        }
    }

    /**
     * An error occurred launching the PayPal browser flow. See [error] for details.
     */
    class Failure(val error: Exception) : PayPalPendingRequest()
}