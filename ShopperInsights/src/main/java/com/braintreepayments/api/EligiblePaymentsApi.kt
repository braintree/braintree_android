package com.braintreepayments.api

import com.braintreepayments.api.EligiblePaymentsApiRequest.Companion.toJson
import org.json.JSONException
import org.json.JSONObject

internal class EligiblePaymentsApi(
    private val braintreeClient: BraintreeClient
) {
    fun execute(request: EligiblePaymentsApiRequest, callback: EligiblePaymentsCallback) {
        val jsonBody = request.toJson()
        val url = "https://api.sandbox.paypal.com/v2/payments/find-eligible-methods"
        braintreeClient.sendPOST(
            url,
            jsonBody,
            object : HttpResponseCallback {
                override fun onResult(responseBody: String?, httpError: Exception?) {
                    if (responseBody != null) {
                        try {
                            val responseJson = JSONObject(responseBody)
                            callback.onResult(EligiblePaymentsApiResult(
                                eligibleMethods = EligiblePaymentMethods(
                                    paypal = EligiblePaymentMethodDetails(
                                        canBeVaulted = true,
                                        eligibleInPayPalNetwork = true,
                                        recommended = true,
                                        recommendedPriority = 1
                                    ),
                                    venmo = EligiblePaymentMethodDetails(
                                        canBeVaulted = true,
                                        eligibleInPayPalNetwork = true,
                                        recommended = true,
                                        recommendedPriority = 1
                                    )
                                )
                            ), null)
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
