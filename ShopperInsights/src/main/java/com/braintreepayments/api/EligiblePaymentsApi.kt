package com.braintreepayments.api

import com.braintreepayments.api.EligiblePaymentsApiRequest.Companion.toJson
import org.json.JSONException

@UnstableApi
internal class EligiblePaymentsApi(
    private val braintreeClient: BraintreeClient
) {
    fun execute(request: EligiblePaymentsApiRequest, callback: EligiblePaymentsCallback) {
        val jsonBody = request.toJson()
        braintreeClient.getConfiguration { configuration, _ ->
            // TODO: Move url to PaypalHttpClient class when it is created
            val baseUrl = when (configuration?.environment) {
                "production" -> "https://api.paypal.com"
                else -> "https://api.sandbox.paypal.com"
            }
            val url = "$baseUrl/v2/payments/find-eligible-methods"
            val additionalHeaders = mapOf(PAYPAL_CLIENT_METADATA_ID to braintreeClient.sessionId)
            braintreeClient.sendPOST(
                url = url,
                data = jsonBody,
                additionalHeaders = additionalHeaders
            ) { responseBody: String?, httpError: Exception? ->
                if (responseBody != null) {
                    try {
                        callback.onResult(
                            EligiblePaymentsApiResult.fromJson(responseBody),
                            null
                        )
                    } catch (e: JSONException) {
                        callback.onResult(null, e)
                    }
                } else {
                    callback.onResult(null, httpError)
                }
            }
        }
    }

    companion object {
        private const val PAYPAL_CLIENT_METADATA_ID = "PayPal-Client-Metadata-Id"
    }
}
