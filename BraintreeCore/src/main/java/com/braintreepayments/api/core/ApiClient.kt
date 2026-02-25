package com.braintreepayments.api.core

import androidx.annotation.RestrictTo
import org.json.JSONException
import org.json.JSONObject

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ApiClient(
    private val braintreeClient: BraintreeClient,
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance,
) {

    suspend fun tokenizeGraphQL(tokenizePayload: JSONObject): JSONObject {
        val responseBody = braintreeClient.sendGraphQLPOST(tokenizePayload)
        val response = parseResponseToJSON(responseBody)
            ?: throw JSONException("Invalid JSON response")
        return response
    }

    suspend fun tokenizeREST(paymentMethod: PaymentMethod): JSONObject {
        val url = versionedPath("$PAYMENT_METHOD_ENDPOINT/${paymentMethod.apiPath}")
        paymentMethod.sessionId = analyticsParamRepository.sessionId

        val responseBody = braintreeClient.sendPOST(
            url = url,
            data = paymentMethod.buildJSON().toString(),
        )

        val response = parseResponseToJSON(responseBody)
            ?: throw JSONException("Invalid JSON response")
        return response
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
