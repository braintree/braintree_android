package com.braintreepayments.api

import androidx.annotation.RestrictTo
import org.json.JSONException
import org.json.JSONObject

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ApiClient(private val braintreeClient: BraintreeClient) {

    fun tokenizeGraphQL(tokenizePayload: JSONObject, callback: TokenizeCallback) =
        braintreeClient.run {
            sendAnalyticsEvent("card.graphql.tokenization.started")
            sendGraphQLPOST(tokenizePayload.toString(), object : HttpResponseCallback {
                override fun onResult(responseBody: String?, httpError: Exception?) {
                    parseResponseToJSON(responseBody)?.let { json ->
                        sendAnalyticsEvent("card.graphql.tokenization.success")
                        callback.onResult(json, null)
                    } ?: httpError?.let { error ->
                        sendAnalyticsEvent("card.graphql.tokenization.failure")
                        callback.onResult(null, error)
                    }
                }
            })
        }

    fun tokenizeREST(paymentMethod: PaymentMethod, callback: TokenizeCallback) =
        braintreeClient.run {
            val url = versionedPath("$PAYMENT_METHOD_ENDPOINT/${paymentMethod.apiPath}")
            paymentMethod.setSessionId(braintreeClient.sessionId)

            sendAnalyticsEvent("card.rest.tokenization.started")
            sendPOST(url, paymentMethod.buildJSON().toString(), object : HttpResponseCallback {
                override fun onResult(responseBody: String?, httpError: Exception?) {
                    parseResponseToJSON(responseBody)?.let { json ->
                        sendAnalyticsEvent("card.rest.tokenization.success")
                        callback.onResult(json, null)
                    } ?: httpError?.let { error ->
                        sendAnalyticsEvent("card.rest.tokenization.failure")
                        callback.onResult(null, error)
                    }
                }
            })
        }

    private fun parseResponseToJSON(responseBody: String?): JSONObject? =
        responseBody?.let {
            try {
                JSONObject(it)
            } catch (e: JSONException) {
                null
            }
        }

    companion object {
        const val PAYMENT_METHOD_ENDPOINT = "payment_methods"

        @JvmStatic
        fun versionedPath(path: String): String {
            return "/v1/$path"
        }
    }
}
