package com.braintreepayments.api

import org.json.JSONException
import java.lang.Exception

/**
 * A pending request for the SEPA Direct Debit web-based authentication flow created by invoking
 * [SEPADirectDebitLauncher.launch]. This pending request should be stored locally within the app or
 * on-device and used to deliver a result of the browser flow in
 * [SEPADirectDebitLauncher.handleReturnToAppFromBrowser]
 */
sealed class SEPADirectDebitPendingRequest {

    /**
     * A pending request was successfully started. This [SEPADirectDebitPendingRequest.Started] should be
     * stored and passed to [SEPADirectDebitLauncher.handleReturnToAppFromBrowser]
     */
    class Started(internal val request: BrowserSwitchPendingRequest.Started) : SEPADirectDebitPendingRequest() {

        /**
         * Convenience constructor to create a [SEPADirectDebitPendingRequest.Started] from your stored
         * [String] obtained from [SEPADirectDebitPendingRequest.Started.toJsonString]
         * @throws [JSONException] if the [jsonString] is invalid
         */
        @Throws(JSONException::class)
        constructor(jsonString: String) : this(BrowserSwitchPendingRequest.Started(jsonString))

        /**
         * Convenience method to return [SEPADirectDebitPendingRequest.Started] in [String] format to be
         * persisted in storage
         */
        fun toJsonString(): String {
            return request.toJsonString()
        }
    }

    /**
     * An error occurred launching the SEPA Direct Debit browser flow. See [error] for details.
     */
    class Failure(val error: Exception) : SEPADirectDebitPendingRequest()
}
