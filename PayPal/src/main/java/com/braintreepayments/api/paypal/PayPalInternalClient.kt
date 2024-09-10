package com.braintreepayments.api.paypal

import android.content.Context
import android.net.Uri
import com.braintreepayments.api.core.ApiClient
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.datacollector.DataCollector
import com.braintreepayments.api.datacollector.DataCollectorInternalRequest
import com.braintreepayments.api.paypal.PayPalPaymentResource.Companion.fromJson
import com.braintreepayments.api.paypal.vaultedit.PayPalVaultEditAuthCallback
import com.braintreepayments.api.paypal.vaultedit.PayPalVaultEditCallback
import com.braintreepayments.api.paypal.vaultedit.PayPalVaultEditRequest
import com.braintreepayments.api.paypal.vaultedit.PayPalVaultEditResult
import com.braintreepayments.api.paypal.vaultedit.PayPalVaultErrorHandlingEditRequest
import org.json.JSONException
import org.json.JSONObject

internal class PayPalInternalClient(
    private val braintreeClient: BraintreeClient,
    private val dataCollector: DataCollector = DataCollector(braintreeClient),
    private val apiClient: ApiClient = ApiClient(braintreeClient)
) {
    private val cancelUrl = "${braintreeClient.appLinkReturnUri}://onetouch/v1/cancel"
    private val successUrl = "${braintreeClient.appLinkReturnUri}://onetouch/v1/success"

    fun sendRequest(
        context: Context,
        payPalRequest: PayPalRequest,
        callback: PayPalInternalClientCallback
    ) {
        braintreeClient.getConfiguration { configuration: Configuration?, configError: Exception? ->
            if (configuration == null) {
                callback.onResult(null, configError)
                return@getConfiguration
            }

            try {
                val endpoint = if (payPalRequest is PayPalVaultRequest) {
                    SETUP_BILLING_AGREEMENT_ENDPOINT
                } else {
                    CREATE_SINGLE_PAYMENT_ENDPOINT
                }
                val url = "/v1/$endpoint"

                val requestBody = payPalRequest.createRequestBody(
                    configuration = configuration,
                    authorization = braintreeClient.authorization,
                    successUrl = successUrl,
                    cancelUrl = cancelUrl
                ) ?: throw JSONException("Error creating requestBody")

                sendPost(
                    url = url,
                    requestBody = requestBody,
                    payPalRequest = payPalRequest,
                    context = context,
                    configuration = configuration,
                    callback = callback
                )
            } catch (exception: JSONException) {
                callback.onResult(null, exception)
            }
        }
    }

    fun tokenize(payPalAccount: PayPalAccount, callback: PayPalInternalTokenizeCallback) {
        apiClient.tokenizeREST(payPalAccount) { tokenizationResponse: JSONObject?, exception: Exception? ->
            if (tokenizationResponse != null) {
                try {
                    callback.onResult(PayPalAccountNonce.fromJSON(tokenizationResponse), null)
                } catch (e: JSONException) {
                    callback.onResult(null, e)
                }
            } else {
                callback.onResult(null, exception)
            }
        }
    }

    private fun sendPost(
        url: String,
        requestBody: String,
        payPalRequest: PayPalRequest,
        context: Context,
        configuration: Configuration,
        callback: PayPalInternalClientCallback
    ) {
        braintreeClient.sendPOST(
            url = url,
            data = requestBody,
        ) { responseBody: String?, httpError: Exception? ->

            if (responseBody == null) {
                callback.onResult(null, httpError)
                return@sendPOST
            }

            try {
                val paypalPaymentResource = fromJson(responseBody)
                val parsedRedirectUri = Uri.parse(paypalPaymentResource.redirectUrl)

                val pairingId = findPairingId(parsedRedirectUri)
                val clientMetadataId = payPalRequest.riskCorrelationId ?: run {
                    val dataCollectorRequest = DataCollectorInternalRequest(
                        payPalRequest.hasUserLocationConsent
                    ).apply {
                        applicationGuid = dataCollector.getPayPalInstallationGUID(context)
                        clientMetadataId = pairingId
                    }
                    dataCollector.getClientMetadataId(
                        context = context,
                        request = dataCollectorRequest,
                        configuration = configuration
                    )
                }

                val paymentAuthRequest = PayPalPaymentAuthRequestParams(
                    payPalRequest = payPalRequest,
                    browserSwitchOptions = null,
                    approvalUrl = parsedRedirectUri.toString(),
                    clientMetadataId = clientMetadataId,
                    pairingId = pairingId,
                    successUrl = successUrl
                )

                callback.onResult(paymentAuthRequest, null)
            } catch (exception: JSONException) {
                callback.onResult(null, exception)
            }
        }
    }

    private fun findPairingId(redirectUri: Uri): String? {
        return redirectUri.getQueryParameter("ba_token")
            ?: redirectUri.getQueryParameter("token")
    }

    @ExperimentalBetaApi
    fun sendVaultEditRequest(
        context: Context,
        payPalVaultEditRequest: PayPalVaultEditRequest,
        callback: PayPalVaultEditCallback,
        riskCorrelationId: String? = null
    ) {
        if (riskCorrelationId != null) {
            sendVaultEditRequestWithRiskCorrelationId(
                context,
                payPalVaultEditRequest,
                riskCorrelationId,
                callback
            )
        } else {
            getClientMetadataId(riskCorrelationId, context) { clientMetadataId ->
                if (clientMetadataId == null) {
                    val result = PayPalVaultEditResult.Failure(BraintreeException("Could not retrieve clientMetaDataId"))
                    callback.onPayPalVaultEditResult(result)
                } else {
                    sendVaultEditRequestWithRiskCorrelationId(
                        context,
                        payPalVaultEditRequest,
                        clientMetadataId,
                        callback
                    )
                }
            }
        }
    }

    @ExperimentalBetaApi
    private fun sendVaultEditRequestWithRiskCorrelationId(
        context: Context,
        payPalVaultEditRequest: PayPalVaultEditRequest,
        riskCorrelationId: String,
        callback: PayPalVaultEditCallback
    ) {
        val params = parameters(payPalVaultEditRequest.editPayPalVaultId).toMutableMap()

        params["risk_correlation_id"] = riskCorrelationId

        val jsonObject = JSONObject(params.toMap())

        braintreeClient.sendPOST(payPalVaultEditRequest.hermesPath, jsonObject.toString()) { response, error ->
            if (error != null) {
                val result = PayPalVaultEditResult.Failure(error)
                callback.onPayPalVaultEditResult(result)
            } else {
                val result = PayPalVaultEditResult.Success(error)
                callback.onPayPalVaultEditResult(result)
            }

            // TODO: use payPalVaultEditAuthCallback
            println("EditFI response: $response")
            println("EditFI error: $error")

            println("EditFI done")
        }
    }

    // TODO: improve method name
    fun parameters(editPayPalVaultId: String): Map<String, Any> {
        val parameters = mutableMapOf<String, Any>()

        parameters["edit_paypal_vault_id"] = editPayPalVaultId
        parameters["return_url"] = successUrl
        parameters["cancel_url"] = cancelUrl


        return parameters
    }

    fun getClientMetadataId(
        riskCorrelationId: String?,
        context: Context,
        callback: (String?) -> Unit
    ) {
        val clientMetadataId = riskCorrelationId ?: run {
            braintreeClient.getConfiguration { configuration: Configuration?, configError: Exception? ->

                // TODO: what to do with hasUserLocationConsent
                val clientMetadataId = dataCollector.getClientMetadataId(context, configuration, false)
                callback(clientMetadataId)
            }
            null
        }

        if (clientMetadataId != null) {
            callback(clientMetadataId)
        }
    }

    @ExperimentalBetaApi
    fun sendVaultEditErrorRequest(payPalVaultEditErrorRequest: PayPalVaultErrorHandlingEditRequest, payPalVaultEditAuthCallback: PayPalVaultEditAuthCallback) {

        val parameters = mutableMapOf<String, Any>()

        fun parameters(): Map<String, Any> {

            parameters["edit_paypal_vault_id"] = payPalVaultEditErrorRequest.editPayPalVaultId
            parameters["return_url"] = successUrl
            parameters["cancel_url"] = cancelUrl
            parameters["correlation_id"] = payPalVaultEditErrorRequest.riskCorrelationId

            return parameters
        }

        val jsonObject = JSONObject(parameters())
        braintreeClient.sendPOST(payPalVaultEditErrorRequest.hermesPath, jsonObject.toString()) { response, error ->
        }
    }

    companion object {
        private const val CREATE_SINGLE_PAYMENT_ENDPOINT = "paypal_hermes/create_payment_resource"
        private const val SETUP_BILLING_AGREEMENT_ENDPOINT = "paypal_hermes/setup_billing_agreement"
    }
}
