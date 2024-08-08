package com.braintreepayments.api.core

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
            sendGraphQLPOST(tokenizePayload) { responseBody, httpError ->
                parseResponseToJSON(responseBody)?.let { json ->
                    callback.onResult(json, null)
                } ?: httpError?.let { error ->
                    callback.onResult(null, error)
                }
            }
        }

    fun tokenizeREST(paymentMethod: PaymentMethod, callback: TokenizeCallback) =
        braintreeClient.run {
            val url = versionedPath("$PAYMENT_METHOD_ENDPOINT/${paymentMethod.apiPath}")
            paymentMethod.sessionId = braintreeClient.sessionId

            sendPOST(
                url = url,
                data = paymentMethod.buildJSON().toString(),
            ) { responseBody, httpError ->
                parseResponseToJSON(responseBody)?.let { json ->
                    callback.onResult(json, null)
                } ?: httpError?.let { error ->
                    callback.onResult(null, error)
                }
            }
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
