package com.braintreepayments.api.paypal

import android.content.Context
import android.net.Uri
import com.braintreepayments.api.core.ApiClient
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.DeviceInspector
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.datacollector.DataCollector
import com.braintreepayments.api.datacollector.DataCollectorInternalRequest
import com.braintreepayments.api.datacollector.DataCollectorRequest
import com.braintreepayments.api.paypal.PayPalPaymentResource.Companion.fromJson
import com.braintreepayments.api.paypal.vaultedit.PayPalInternalClientEditCallback
import com.braintreepayments.api.paypal.vaultedit.PayPalVaultEditAuthRequestParams
import com.braintreepayments.api.paypal.vaultedit.PayPalVaultEditRequest
import com.braintreepayments.api.paypal.vaultedit.PayPalVaultErrorHandlingEditRequest
import org.json.JSONException
import org.json.JSONObject

@Suppress("TooManyFunctions")
internal class PayPalInternalClient(
    private val braintreeClient: BraintreeClient,
    private val dataCollector: DataCollector = DataCollector(braintreeClient),
    private val apiClient: ApiClient = ApiClient(braintreeClient),
    private val deviceInspector: DeviceInspector = DeviceInspector()
) {
    private val cancelUrl = "${braintreeClient.appLinkReturnUri}://onetouch/v1/cancel"
    private val successUrl = "${braintreeClient.appLinkReturnUri}://onetouch/v1/success"
    private val appLink = braintreeClient.appLinkReturnUri?.toString()

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
                val isBillingAgreement = payPalRequest is PayPalVaultRequest
                val endpoint = if (isBillingAgreement) {
                    SETUP_BILLING_AGREEMENT_ENDPOINT
                } else {
                    CREATE_SINGLE_PAYMENT_ENDPOINT
                }
                val url = "/v1/$endpoint"
                val appLinkReturn = if (isBillingAgreement) appLink else null

                if (isBillingAgreement && (payPalRequest as PayPalVaultRequest).enablePayPalAppSwitch) {
                    payPalRequest.enablePayPalAppSwitch = isPayPalInstalled(context)
                }

                val requestBody = payPalRequest.createRequestBody(
                    configuration = configuration,
                    authorization = braintreeClient.authorization,
                    successUrl = successUrl,
                    cancelUrl = cancelUrl,
                    appLink = appLinkReturn
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
                    clientMetadataId = clientMetadataId,
                    pairingId = pairingId,
                    successUrl = successUrl
                )

                if (isAppSwitchEnabled(payPalRequest) && isPayPalInstalled(context)) {
                    if (!pairingId.isNullOrEmpty()) {
                        paymentAuthRequest.approvalUrl = createAppSwitchUri(parsedRedirectUri).toString()
                    } else {
                        callback.onResult(null, BraintreeException("Missing BA Token for PayPal App Switch."))
                    }
                } else {
                    paymentAuthRequest.approvalUrl = parsedRedirectUri.toString()
                }

                callback.onResult(paymentAuthRequest, null)
            } catch (exception: JSONException) {
                callback.onResult(null, exception)
            }
        }
    }

    private fun createAppSwitchUri(uri: Uri): Uri {
        return uri.buildUpon()
            .appendQueryParameter("source", "braintree_sdk")
            .appendQueryParameter("switch_initiated_time", System.currentTimeMillis().toString())
            .build()
    }

    fun isAppSwitchEnabled(payPalRequest: PayPalRequest): Boolean {
        return (payPalRequest is PayPalVaultRequest) &&
                payPalRequest.enablePayPalAppSwitch
    }

    fun isPayPalInstalled(context: Context): Boolean {
        return deviceInspector.isPayPalInstalled(context)
    }

    private fun findPairingId(redirectUri: Uri): String? {
        return redirectUri.getQueryParameter("ba_token")
            ?: redirectUri.getQueryParameter("token")
    }

    @ExperimentalBetaApi
    fun sendVaultEditRequest(
        context: Context,
        request: PayPalVaultEditRequest,
        callback: PayPalInternalClientEditCallback
    ) {
        val riskCorrelationId = (request as? PayPalVaultErrorHandlingEditRequest)?.riskCorrelationId

        getClientMetadataId(
            context,
            riskCorrelationId
        ) { clientMetadataId ->
            if (clientMetadataId == null) {
                callback.onPayPalVaultEditResult(null, BraintreeException("An unexpected error occurred"))
            } else {
                val riskCorrelationId =
                    (request as? PayPalVaultErrorHandlingEditRequest)
                        ?.riskCorrelationId ?: clientMetadataId

                sendVaultEditRequestWithRiskCorrelationId(
                    request,
                    riskCorrelationId,
                    callback
                )
            }
        }
    }

    @ExperimentalBetaApi
    private fun sendVaultEditRequestWithRiskCorrelationId(
        payPalVaultEditRequest: PayPalVaultEditRequest,
        riskCorrelationId: String,
        callback: PayPalInternalClientEditCallback
    ) {
        val params = editFiParameters(payPalVaultEditRequest.editPayPalVaultId).toMutableMap()

        params["risk_correlation_id"] = riskCorrelationId

        val jsonObject = JSONObject(params.toMap())

        braintreeClient.sendPOST(
            payPalVaultEditRequest.hermesPath,
            jsonObject.toString()
        ) { response, error ->
            if (error != null) {
                callback.onPayPalVaultEditResult(null, error)
            } else {
                try {
                    val responseBody = JSONObject(response)
                    val agreementSetup = responseBody.getJSONObject("agreementSetup")

                    val params = PayPalVaultEditAuthRequestParams(
                        riskCorrelationId,
                        null,
                        agreementSetup.getString("approvalUrl"),
                        successUrl
                    )

                    callback.onPayPalVaultEditResult(params, null)
                } catch (jsonException: JSONException) {
                    callback.onPayPalVaultEditResult(null, jsonException)
                }
            }
        }
    }

    private fun editFiParameters(editPayPalVaultId: String): Map<String, Any> {
        val parameters = mutableMapOf<String, Any>()

        parameters["edit_paypal_vault_id"] = editPayPalVaultId
        parameters["return_url"] = successUrl
        parameters["cancel_url"] = cancelUrl

        return parameters
    }

    private fun getClientMetadataId(
        context: Context,
        correlationId: String?,
        callback: (String?) -> Unit
    ) {
        braintreeClient.getConfiguration { configuration, error ->
            if (error != null) {
                callback(error("No Client Metadata Id"))
            } else {
                val request = DataCollectorRequest(false, correlationId)

                val dataCollectorRequest = DataCollectorInternalRequest(
                    false
                ).apply {
                    applicationGuid = dataCollector.getPayPalInstallationGUID(context)
                    clientMetadataId = correlationId
                }
                val clientMetadataId = dataCollector.getClientMetadataId(
                    context = context,
                    request = dataCollectorRequest,
                    configuration = configuration
                )

                callback(clientMetaDataId)
            }
        }
    }

    companion object {
        private const val CREATE_SINGLE_PAYMENT_ENDPOINT = "paypal_hermes/create_payment_resource"
        private const val SETUP_BILLING_AGREEMENT_ENDPOINT = "paypal_hermes/setup_billing_agreement"
    }
}
