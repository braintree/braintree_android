package com.braintreepayments.api.paypal

import android.content.Context
import android.net.Uri
import com.braintreepayments.api.core.ApiClient
import com.braintreepayments.api.core.AppSwitchRepository
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.DeviceInspector
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.core.GetReturnLinkUseCase
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.core.SetAppSwitchUseCase
import com.braintreepayments.api.datacollector.DataCollector
import com.braintreepayments.api.datacollector.DataCollectorInternalRequest
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
    private val deviceInspector: DeviceInspector = DeviceInspector(),
    private val merchantRepository: MerchantRepository = MerchantRepository.instance,
    private val getReturnLinkUseCase: GetReturnLinkUseCase = GetReturnLinkUseCase(merchantRepository),
    private val setAppSwitchUseCase: SetAppSwitchUseCase = SetAppSwitchUseCase(AppSwitchRepository.instance),
    private val payPalTokenResponseRepository: PayPalTokenResponseRepository = PayPalTokenResponseRepository.instance,
    private val payPalSetPaymentTokenUseCase: PayPalSetPaymentTokenUseCase = PayPalSetPaymentTokenUseCase(
        payPalTokenResponseRepository
    )
) {

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

                if (payPalRequest.enablePayPalAppSwitch) {
                    payPalRequest.enablePayPalAppSwitch = isPayPalInstalled(context)
                }

                val returnLinkResult = getReturnLinkUseCase()
                val navigationLink: String = when (returnLinkResult) {
                    is GetReturnLinkUseCase.ReturnLinkResult.AppLink -> returnLinkResult.appLinkReturnUri.toString()
                    is GetReturnLinkUseCase.ReturnLinkResult.DeepLink -> returnLinkResult.deepLinkFallbackUrlScheme
                    is GetReturnLinkUseCase.ReturnLinkResult.Failure -> {
                        callback.onResult(null, returnLinkResult.exception)
                        return@getConfiguration
                    }
                }
                val appLinkParam = if (
                    returnLinkResult is GetReturnLinkUseCase.ReturnLinkResult.AppLink
                ) {
                    merchantRepository.appLinkReturnUri?.toString()
                } else {
                    null
                }

                val cancelUrl = "$navigationLink://onetouch/v1/cancel"
                val successUrl = "$navigationLink://onetouch/v1/success"

                val requestBody = payPalRequest.createRequestBody(
                    configuration = configuration,
                    authorization = merchantRepository.authorization,
                    successUrl = successUrl,
                    cancelUrl = cancelUrl,
                    appLink = appLinkParam
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
            url = url, data = requestBody,
        ) { responseBody: String?, httpError: Exception? ->
            if (responseBody == null) {
                callback.onResult(null, httpError)
                return@sendPOST
            }
            try {
                val paypalPaymentResource = PayPalPaymentResource.fromJson(responseBody)
                val parsedRedirectUri = Uri.parse(paypalPaymentResource.redirectUrl)
                setAppSwitchUseCase(paypalPaymentResource.isAppSwitchFlow)
                val paypalContextId = extractPayPalContextId(parsedRedirectUri)
                payPalSetPaymentTokenUseCase.setPaymentToken(paypalContextId)
                val clientMetadataId = payPalRequest.riskCorrelationId ?: run {
                    val dataCollectorRequest = DataCollectorInternalRequest(
                        payPalRequest.hasUserLocationConsent
                    ).apply {
                        applicationGuid = dataCollector.getPayPalInstallationGUID(context)
                        clientMetadataId = paypalContextId
                    }

                       dataCollector.getClientMetadataId(context, dataCollectorRequest, configuration)
                }
                val returnLink: String = when (val returnLinkResult = getReturnLinkUseCase()) {
                    is GetReturnLinkUseCase.ReturnLinkResult.AppLink -> returnLinkResult.appLinkReturnUri.toString()
                    is GetReturnLinkUseCase.ReturnLinkResult.DeepLink -> returnLinkResult.deepLinkFallbackUrlScheme
                    is GetReturnLinkUseCase.ReturnLinkResult.Failure -> {
                        callback.onResult(null, returnLinkResult.exception)
                        return@sendPOST
                    }
                }
                val paymentAuthRequest = PayPalPaymentAuthRequestParams(
                    payPalRequest = payPalRequest,
                    browserSwitchOptions = null,
                    clientMetadataId = clientMetadataId,
                    paypalContextId = paypalContextId,
                    successUrl = "$returnLink://onetouch/v1/success"
                )
                if (isAppSwitchEnabled(payPalRequest) && isPayPalInstalled(context)) {
                    if (!paypalContextId.isNullOrEmpty()) {
                        paymentAuthRequest.approvalUrl =
                            createAppSwitchUri(parsedRedirectUri).toString()
                    } else {
                        callback.onResult(
                            null, BraintreeException("Missing Token for PayPal App Switch.")
                        )
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

    fun isAppSwitchEnabled(payPalRequest: PayPalRequest) = payPalRequest.enablePayPalAppSwitch

    fun isPayPalInstalled(context: Context): Boolean {
        return deviceInspector.isPayPalInstalled(context)
    }

    private fun extractPayPalContextId(redirectUri: Uri): String? {
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
            request.hasUserLocationConsent,
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

        val returnLink: String = when (val returnLinkResult = getReturnLinkUseCase()) {
            is GetReturnLinkUseCase.ReturnLinkResult.AppLink -> returnLinkResult.appLinkReturnUri.toString()
            is GetReturnLinkUseCase.ReturnLinkResult.DeepLink -> returnLinkResult.deepLinkFallbackUrlScheme
            is GetReturnLinkUseCase.ReturnLinkResult.Failure -> {
                callback.onPayPalVaultEditResult(null, returnLinkResult.exception)
                return
            }
        }

        val params = editFiParameters(payPalVaultEditRequest.editPayPalVaultId, returnLink).toMutableMap()

        params["correlation_id"] = riskCorrelationId

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
                        "$returnLink://onetouch/v1/success"
                    )

                    callback.onPayPalVaultEditResult(params, null)
                } catch (jsonException: JSONException) {
                    callback.onPayPalVaultEditResult(null, jsonException)
                }
            }
        }
    }

    private fun editFiParameters(editPayPalVaultId: String, returnLink: String): Map<String, Any> {
        val parameters = mutableMapOf<String, Any>()

        parameters["edit_paypal_vault_id"] = editPayPalVaultId
        parameters["return_url"] = "$returnLink://onetouch/v1/success"
        parameters["cancel_url"] = "$returnLink://onetouch/v1/cancel"

        return parameters
    }

    private fun getClientMetadataId(
        context: Context,
        hasUserLocationConsent: Boolean,
        correlationId: String?,
        callback: (String?) -> Unit
    ) {
        braintreeClient.getConfiguration { configuration, error ->
            if (error != null) {
                callback(error("No Client Metadata Id"))
            } else {
                val dataCollectorRequest = DataCollectorInternalRequest(
                    hasUserLocationConsent
                ).apply {
                    applicationGuid = dataCollector.getPayPalInstallationGUID(context)
                    clientMetadataId = correlationId
                }
                val clientMetadataId = dataCollector.getClientMetadataId(
                    context = context,
                    request = dataCollectorRequest,
                    configuration = configuration
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
