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
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val coroutineScope: CoroutineScope = CoroutineScope(mainDispatcher)
) {

    fun tokenizeGraphQL(tokenizePayload: JSONObject, callback: TokenizeCallback) =
        braintreeClient.run {
            coroutineScope.launch {
                try {
                    val responseBody = sendGraphQLPOST(tokenizePayload)
                    parseResponseToJSON(responseBody)?.let { json ->
                        callback.onResult(json, null)
                    } ?: callback.onResult(
                        null,
                        BraintreeException("Unable to parse GraphQL response.")
                    )
                } catch (e: IOException) {
                    callback.onResult(null, e)
                }
            }
        }

    fun tokenizeREST(paymentMethod: PaymentMethod, callback: TokenizeCallback) =
        braintreeClient.run {
            val url = versionedPath("$PAYMENT_METHOD_ENDPOINT/${paymentMethod.apiPath}")
            paymentMethod.sessionId = analyticsParamRepository.sessionId

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
