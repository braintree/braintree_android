package com.braintreepayments.api.core

import androidx.annotation.RestrictTo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class ApiClient(
    private val braintreeClient: BraintreeClient,
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val coroutineScope: CoroutineScope = CoroutineScope(dispatcher)
) {

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
            paymentMethod.sessionId = analyticsParamRepository.sessionId

            coroutineScope.launch {
                try {
                    val responseBody = sendPOST(
                        url = url,
                        data = paymentMethod.buildJSON().toString(),
                    )
                    parseResponseToJSON(responseBody)?.let { json ->
                        callback.onResult(json, null)
                    } ?: callback.onResult(null, JSONException("Invalid JSON response"))
                } catch (httpError: IOException) {
                    callback.onResult(null, httpError)
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
