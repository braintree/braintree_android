package com.braintreepayments.api

import androidx.annotation.RestrictTo
import org.json.JSONException
import org.json.JSONObject

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ApiClient constructor(private val braintreeClient: BraintreeClient) {

    fun tokenizeGraphQL(tokenizePayload: JSONObject, callback: TokenizeCallback) {
        braintreeClient.sendAnalyticsEvent("card.graphql.tokenization.started")
        braintreeClient.sendGraphQLPOST(tokenizePayload.toString()) { responseBody, httpError ->
            if (responseBody != null) {
                try {
                    callback.onResult(JSONObject(responseBody), null)
                    braintreeClient.sendAnalyticsEvent("card.graphql.tokenization.success")
                } catch (exception: JSONException) {
                    braintreeClient.sendAnalyticsEvent("card.graphql.tokenization.failure")
                    callback.onResult(null, exception)
                }
            } else {
                braintreeClient.sendAnalyticsEvent("card.graphql.tokenization.failure")
                callback.onResult(null, httpError)
            }
        }
    }

    fun tokenizeREST(paymentMethod: PaymentMethod, callback: TokenizeCallback) {
        val url = versionedPath("$PAYMENT_METHOD_ENDPOINT/${paymentMethod.apiPath}")
        paymentMethod.setSessionId(braintreeClient.sessionId)

        try {
            val body = paymentMethod.buildJSON().toString()
            braintreeClient.sendPOST(url, body) { responseBody, httpError ->
                if (responseBody != null) {
                    try {
                        callback.onResult(JSONObject(responseBody), null)
                    } catch (exception: JSONException) {
                        callback.onResult(null, exception)
                    }
                } else {
                    callback.onResult(null, httpError)
                }
            }
        } catch (exception: JSONException) {
            callback.onResult(null, exception)
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