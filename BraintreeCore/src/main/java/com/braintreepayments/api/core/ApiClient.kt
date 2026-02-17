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
        return braintreeClient.run {
            val responseBody = sendGraphQLPOST(tokenizePayload)
            parseResponseToJSON(responseBody)
                ?: throw JSONException("Invalid JSON response")
        }
    }

    suspend fun tokenizeREST(paymentMethod: PaymentMethod): JSONObject {
        return braintreeClient.run {
            val url = versionedPath("$PAYMENT_METHOD_ENDPOINT/${paymentMethod.apiPath}")
            paymentMethod.sessionId = analyticsParamRepository.sessionId

            val responseBody = sendPOST(
                url = url,
                data = paymentMethod.buildJSON().toString(),
            )

            parseResponseToJSON(responseBody)
                ?: throw JSONException("Invalid JSON response")
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