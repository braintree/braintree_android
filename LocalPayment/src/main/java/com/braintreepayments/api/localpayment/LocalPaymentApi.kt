package com.braintreepayments.api.localpayment

import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.localpayment.LocalPaymentNonce.Companion.fromJSON
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

internal class LocalPaymentApi(
    private val braintreeClient: BraintreeClient,
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance,
    private val merchantRepository: MerchantRepository = MerchantRepository.instance,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val coroutineScope: CoroutineScope = CoroutineScope(dispatcher)
) {

    fun createPaymentMethod(
        request: LocalPaymentRequest,
        callback: LocalPaymentInternalAuthRequestCallback
    ) {
        val returnUrlScheme = braintreeClient.getReturnUrlScheme()
        val returnUrl = "$returnUrlScheme://${LocalPaymentClient.LOCAL_PAYMENT_SUCCESS}"
        val cancel = "$returnUrlScheme://${LocalPaymentClient.LOCAL_PAYMENT_CANCEL}"

        val url = "/v1/local_payments/create"

        coroutineScope.launch {
            try {
                val responseBody = braintreeClient.sendPOST(
                    url,
                    request.build(returnUrl, cancel)
                )
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
            } catch (httpError: IOException) {
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
            coroutineScope.launch {
                try {
                    val responseBody = braintreeClient.sendPOST(
                        url = url,
                        data = payload.toString()
                    )
                    try {
                        val result =
                            fromJSON(JSONObject(responseBody))
                        callback.onResult(result, null)
                    } catch (jsonException: JSONException) {
                        callback.onResult(null, jsonException)
                    }
                } catch (httpError: IOException) {
                    callback.onResult(null, httpError)
                }
            }
        } catch (ignored: JSONException) { /* do nothing */
        }
    }
}
