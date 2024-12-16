package com.braintreepayments.api.localpayment

import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.localpayment.LocalPaymentNonce.Companion.fromJSON
import org.json.JSONException
import org.json.JSONObject

internal class LocalPaymentApi(
    private val braintreeClient: BraintreeClient,
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance,
    private val merchantRepository: MerchantRepository = MerchantRepository.instance,
) {

    fun createPaymentMethod(
        request: LocalPaymentRequest,
        callback: LocalPaymentInternalAuthRequestCallback
    ) {
        val returnUrlScheme = braintreeClient.getReturnUrlScheme()
        val returnUrl = "$returnUrlScheme://${LocalPaymentClient.LOCAL_PAYMENT_SUCCESS}"
        val cancel = "$returnUrlScheme://${LocalPaymentClient.LOCAL_PAYMENT_CANCEL}"

        val url = "/v1/local_payments/create"

        braintreeClient.sendPOST(
            url = url,
            data = request.build(returnUrl, cancel)
        ) { responseBody: String?, httpError: Exception? ->
            if (responseBody != null) {
                try {
                    val responseJson = JSONObject(responseBody)
                    val redirectUrl = responseJson.getJSONObject("paymentResource")
                        .getString("redirectUrl")
                    val paymentToken = responseJson.getJSONObject("paymentResource")
                        .getString("paymentToken")

                    val transaction =
                        LocalPaymentAuthRequestParams(request, redirectUrl, paymentToken)
                    callback.onLocalPaymentInternalAuthResult(transaction, null)
                } catch (e: JSONException) {
                    callback.onLocalPaymentInternalAuthResult(null, e)
                }
            } else {
                callback.onLocalPaymentInternalAuthResult(null, httpError)
            }
        }
    }

    fun tokenize(
        merchantAccountId: String?,
        responseString: String?,
        clientMetadataID: String?,
        callback: LocalPaymentInternalTokenizeCallback
    ) {
        val payload = JSONObject()

        try {
            payload.put("merchant_account_id", merchantAccountId)

            val paypalAccount = JSONObject()
                .put("intent", "sale")
                .put("response", JSONObject().put("webURL", responseString))
                .put("options", JSONObject().put("validate", false))
                .put("response_type", "web")
                .put("correlation_id", clientMetadataID)
            payload.put("paypal_account", paypalAccount)

            val metaData = JSONObject()
                .put("source", "client")
                .put("integration", merchantRepository.integrationType.stringValue)
                .put("sessionId", analyticsParamRepository.sessionId)
            payload.put("_meta", metaData)

            val url = "/v1/payment_methods/paypal_accounts"
            braintreeClient.sendPOST(
                url = url,
                data = payload.toString()
            ) { responseBody: String?, httpError: Exception? ->
                if (responseBody != null) {
                    try {
                        val result =
                            fromJSON(JSONObject(responseBody))
                        callback.onResult(result, null)
                    } catch (jsonException: JSONException) {
                        callback.onResult(null, jsonException)
                    }
                } else {
                    callback.onResult(null, httpError)
                }
            }
        } catch (ignored: JSONException) { /* do nothing */
        }
    }
}
