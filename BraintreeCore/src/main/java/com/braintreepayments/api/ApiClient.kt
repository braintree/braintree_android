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
            parseResponseToJSON(
                responseBody,
                httpError,
                callback,
                isGraphQL = true
            )
        }
    }

    fun tokenizeREST(paymentMethod: PaymentMethod, callback: TokenizeCallback) {
        val url = versionedPath("$PAYMENT_METHOD_ENDPOINT/${paymentMethod.apiPath}")
        paymentMethod.setSessionId(braintreeClient.sessionId)

        try {
            val body = paymentMethod.buildJSON().toString()
            braintreeClient.sendPOST(url, body) { responseBody, httpError ->
                parseResponseToJSON(responseBody, httpError, callback)
            }
        } catch (exception: JSONException) {
            callback.onResult(null, exception)
        }
    }

    private fun parseResponseToJSON(
        responseBody: String?,
        exception: Exception?,
        callback: TokenizeCallback,
        isGraphQL: Boolean = false) {
        responseBody?.also {
            try {
                callback.onResult(JSONObject(it), null)
                if (isGraphQL)
                    braintreeClient.sendAnalyticsEvent("card.graphql.tokenization.success")
            } catch (exception: JSONException) {
                if (isGraphQL)
                    braintreeClient.sendAnalyticsEvent("card.graphql.tokenization.failure")
                callback.onResult(null, exception)
            }
        } ?: run {
            if (isGraphQL)
                braintreeClient.sendAnalyticsEvent("card.graphql.tokenization.failure")
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