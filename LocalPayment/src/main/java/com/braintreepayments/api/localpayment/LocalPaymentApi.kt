package com.braintreepayments.api.localpayment

import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.localpayment.LocalPaymentNonce.Companion.fromJSON
import org.json.JSONObject

internal class LocalPaymentApi(
    private val braintreeClient: BraintreeClient,
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance,
    private val merchantRepository: MerchantRepository = MerchantRepository.instance
) {

    suspend fun createPaymentMethod(
        request: LocalPaymentRequest
    ): LocalPaymentAuthRequestParams {
        val returnUrlScheme = braintreeClient.getReturnUrlScheme()
        val returnUrl = "$returnUrlScheme://${LocalPaymentClient.LOCAL_PAYMENT_SUCCESS}"
        val cancel = "$returnUrlScheme://${LocalPaymentClient.LOCAL_PAYMENT_CANCEL}"

        val url = "/v1/local_payments/create"
        val responseBody = braintreeClient.sendPOST(
            url,
            request.build(returnUrl, cancel)
        )

        val responseJson = JSONObject(responseBody)
        val redirectUrl = responseJson.getJSONObject("paymentResource")
            .getString("redirectUrl")
        val paymentToken = responseJson.getJSONObject("paymentResource")
            .getString("paymentToken")

        return LocalPaymentAuthRequestParams(request, redirectUrl, paymentToken)
    }

    suspend fun tokenize(
        merchantAccountId: String?,
        responseString: String?,
        clientMetadataID: String?
    ): LocalPaymentNonce {
        val payload = JSONObject()
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
        val responseBody = braintreeClient.sendPOST(
            url = url,
            data = payload.toString()
        )
        return fromJSON(JSONObject(responseBody))
    }
}
