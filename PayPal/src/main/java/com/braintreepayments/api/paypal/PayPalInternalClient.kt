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
import com.braintreepayments.api.paypal.vaultedit.EditFIAgreementSetup
import com.braintreepayments.api.paypal.vaultedit.PayPalVaultEditAuthRequest
import com.braintreepayments.api.paypal.vaultedit.PayPalVaultEditCallback
import com.braintreepayments.api.paypal.vaultedit.PayPalVaultEditRequest
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

    private val editFiCancelUrl = "${braintreeClient.appLinkReturnUri}"
    private val editFiSuccessUrl = "${braintreeClient.appLinkReturnUri}"

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
        callback: PayPalVaultEditCallback
    ) {
        getClientMetadataId(
            context
        ) { clientMetadataId ->
            if (clientMetadataId == null) {
                val result = PayPalVaultEditAuthRequest.Failure(
                    BraintreeException("Could not retrieve clientMetaDataId")
                )
                callback.onPayPalVaultEditResult(result)
            } else {

                val riskCorrelationId = (payPalVaultEditRequest as? PayPalVaultErrorHandlingEditRequest)?.riskCorrelationId ?: clientMetadataId

                sendVaultEditRequestWithRiskCorrelationId(
                    context,
                    payPalVaultEditRequest,
                    riskCorrelationId,
                    callback
                )
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

        braintreeClient.sendPOST(
            payPalVaultEditRequest.hermesPath,
            jsonObject.toString()
        ) { response, error ->
            if (error != null) {
                val result = PayPalVaultEditAuthRequest.Failure(error)
                callback.onPayPalVaultEditResult(result)
            } else {
                try {
                    val responseBody = JSONObject(response)
                    val agreementSetup = responseBody.getJSONObject("agreementSetup")

                    val editFIAgreementSetup = EditFIAgreementSetup(
                        agreementSetup.getString("tokenId"),
                        agreementSetup.getString("approvalUrl"),
                        agreementSetup.getString("paypalAppApprovalUrl")
                    )

                    val result = PayPalVaultEditAuthRequest.ReadyToLaunch(
                        riskCorrelationId,
                        editFIAgreementSetup
                    )
                    callback.onPayPalVaultEditResult(result)
                } catch (jsonException: JSONException) {
                    val result = PayPalVaultEditAuthRequest.Failure(jsonException)
                    callback.onPayPalVaultEditResult(result)
                }
            }
        }
    }

    // TODO: improve method name
    fun parameters(editPayPalVaultId: String): Map<String, Any> {
        val parameters = mutableMapOf<String, Any>()

        parameters["edit_paypal_vault_id"] = editPayPalVaultId
        parameters["return_url"] = editFiSuccessUrl
        parameters["cancel_url"] = editFiCancelUrl

        return parameters
    }

    private fun getClientMetadataId(
        context: Context,
        callback: (String?) -> Unit
    ) {
        braintreeClient.getConfiguration { configuration, error ->

            if (error != null) {
                callback(error("No Client Metadata Id"))
            } else {

                // TODO: what to do with hasUserLocationConsent
                val clientMetadataId = dataCollector.getClientMetadataId(
                    context, configuration,
                    false
                )

                callback(clientMetadataId)
            }
        }
    }

    companion object {
        private const val CREATE_SINGLE_PAYMENT_ENDPOINT = "paypal_hermes/create_payment_resource"
        private const val SETUP_BILLING_AGREEMENT_ENDPOINT = "paypal_hermes/setup_billing_agreement"
    }
}
