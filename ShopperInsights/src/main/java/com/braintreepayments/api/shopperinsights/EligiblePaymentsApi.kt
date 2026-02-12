package com.braintreepayments.api.shopperinsights

import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.shopperinsights.EligiblePaymentsApiRequest.Companion.toJson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import java.io.IOException

internal class EligiblePaymentsApi(
    private val braintreeClient: BraintreeClient,
    private val analyticsParamRepository: AnalyticsParamRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val coroutineScope: CoroutineScope = CoroutineScope(dispatcher)
) {
    fun execute(request: EligiblePaymentsApiRequest, callback: EligiblePaymentsCallback) {
        val jsonBody = request.toJson()
        coroutineScope.launch {
            try {
                val configuration = braintreeClient.getConfiguration()
                // TODO: Move url to PaypalHttpClient class when it is created
                val baseUrl = when (configuration.environment) {
                    "production" -> "https://api.paypal.com"
                    else -> "https://api.sandbox.paypal.com"
                }
                val url = "$baseUrl/v2/payments/find-eligible-methods"
                val additionalHeaders = mapOf(PAYPAL_CLIENT_METADATA_ID to analyticsParamRepository.sessionId)
                val responseBody = braintreeClient.sendPOST(
                    url = url,
                    data = jsonBody,
                    additionalHeaders = additionalHeaders
                )
                try {
                    callback.onResult(
                        result = EligiblePaymentsApiResult.fromJson(responseBody),
                        error = null
                    )
                } catch (e: JSONException) {
                    callback.onResult(null, e)
                }
            } catch (e: IOException) {
                callback.onResult(null, e)
            }
        }
    }

    companion object {
        private const val PAYPAL_CLIENT_METADATA_ID = "PayPal-Client-Metadata-Id"
    }
}
