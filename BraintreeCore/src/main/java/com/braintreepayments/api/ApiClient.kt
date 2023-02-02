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
            parseResponse(
                responseBody,
                httpError,
                callback,
                analyticsEventName = "card.graphql.tokenization"
            )
        }
    }

    fun tokenizeREST(paymentMethod: PaymentMethod, callback: TokenizeCallback) {
        val url = versionedPath("$PAYMENT_METHOD_ENDPOINT/${paymentMethod.apiPath}")
        paymentMethod.setSessionId(braintreeClient.sessionId)

        try {
            val body = paymentMethod.buildJSON().toString()
            braintreeClient.sendPOST(url, body) { responseBody, httpError ->
                parseResponse(responseBody, httpError, callback)
            }
        } catch (exception: JSONException) {
            returnError(callback, exception)
        }
    }

    private fun parseResponse(
        responseBody: String?,
        exception: Exception?,
        callback: TokenizeCallback,
        analyticsEventName: String? = null) {
        responseBody?.also {
            try {
                returnSuccess(callback, JSONObject(it))
                analyticsEventName?.also { event ->
                    braintreeClient.sendAnalyticsEvent("$event.success")
                }
            } catch (exception: JSONException) {
                analyticsEventName?.also { event ->
                    braintreeClient.sendAnalyticsEvent("$event.failure")
                }
                returnError(callback, exception)
            }
        } ?: run {
            analyticsEventName?.also { event ->
                braintreeClient.sendAnalyticsEvent("$event.failure")
            }
            returnError(callback, exception)
        }
    }

    private fun returnSuccess(callback: TokenizeCallback, jsonObject: JSONObject?) {
        callback.onResult(jsonObject, null)
    }

    private fun returnError(callback: TokenizeCallback, exception: Exception?) {
        callback.onResult(null, exception)
    }

    companion object {
        const val PAYMENT_METHOD_ENDPOINT = "payment_methods"

        @JvmStatic
        fun versionedPath(path: String): String {
            return "/v1/$path"
        }
    }
}