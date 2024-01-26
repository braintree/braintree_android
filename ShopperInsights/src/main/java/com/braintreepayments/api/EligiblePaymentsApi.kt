package com.braintreepayments.api

import com.braintreepayments.api.EligiblePaymentsApiRequest.Companion.toJson
import org.json.JSONException

internal class EligiblePaymentsApi(
    private val braintreeClient: BraintreeClient
) {
    fun execute(request: EligiblePaymentsApiRequest, callback: EligiblePaymentsCallback) {
        val jsonBody = request.toJson()
        // TODO: Move url to PaypalHttpClient class when it is created
        val url = "https://api.sandbox.paypal.com/v2/payments/find-eligible-methods"
        braintreeClient.sendPOST(
            url,
            jsonBody,
            object : HttpResponseCallback {
                override fun onResult(responseBody: String?, httpError: Exception?) {
                    if (httpError != null) {
                        callback.onResult(null, httpError)
                    } else if (responseBody != null) {
                        try {
                            callback.onResult(
                                EligiblePaymentsApiResult.fromJson(responseBody),
                                null
                            )
                        } catch (e: JSONException) {
                            callback.onResult(null, e)
                        }
                    } else {
                        callback.onResult(null, Exception("null body"))
                    }
                }
            }
        )
    }
}
