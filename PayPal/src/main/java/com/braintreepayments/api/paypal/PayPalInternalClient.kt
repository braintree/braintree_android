package com.braintreepayments.api.paypal

import android.content.Context
import android.net.Uri
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.ApiClient
import com.braintreepayments.api.core.AppSwitchRepository
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.DeviceInspector
import com.braintreepayments.api.core.DeviceInspectorProvider
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.core.SetAppSwitchUseCase
import com.braintreepayments.api.core.usecase.GetAppSwitchUseCase
import com.braintreepayments.api.core.usecase.GetReturnLinkUseCase
import com.braintreepayments.api.datacollector.DataCollector
import com.braintreepayments.api.datacollector.DataCollectorInternalRequest
import org.json.JSONException
import org.json.JSONObject

internal class PayPalInternalClient(
    private val braintreeClient: BraintreeClient,
    private val dataCollector: DataCollector = DataCollector(braintreeClient),
    private val apiClient: ApiClient = ApiClient(braintreeClient),
    private val deviceInspector: DeviceInspector = DeviceInspectorProvider().deviceInspector,
    private val merchantRepository: MerchantRepository = MerchantRepository.instance,
    private val getReturnLinkUseCase: GetReturnLinkUseCase = GetReturnLinkUseCase(merchantRepository),
    private val setAppSwitchUseCase: SetAppSwitchUseCase = SetAppSwitchUseCase(AppSwitchRepository.instance),
    private val getAppSwitchUseCase: GetAppSwitchUseCase = GetAppSwitchUseCase(AppSwitchRepository.instance),
    private val resolvePayPalUseCase: ResolvePayPalUseCase = ResolvePayPalUseCase(merchantRepository),
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance,
) {

    fun sendRequest(
        context: Context,
        payPalRequest: PayPalRequest,
        configuration: Configuration,
        callback: PayPalInternalClientCallback
    ) {
        try {
            val endpoint = if (payPalRequest.isBillingAgreement()) {
                SETUP_BILLING_AGREEMENT_ENDPOINT
            } else {
                CREATE_SINGLE_PAYMENT_ENDPOINT
            }
            val url = "/v1/$endpoint"

            if (payPalRequest.enablePayPalAppSwitch) {
                payPalRequest.enablePayPalAppSwitch =
                    deviceInspector.isPayPalInstalled() && resolvePayPalUseCase()
            }

            val returnLinkResult = getReturnLinkUseCase()
            val navigationLink: String = when (returnLinkResult) {
                is GetReturnLinkUseCase.ReturnLinkResult.AppLink -> returnLinkResult.appLinkReturnUri.toString()
                is GetReturnLinkUseCase.ReturnLinkResult.DeepLink -> returnLinkResult.deepLinkFallbackUrlScheme
                is GetReturnLinkUseCase.ReturnLinkResult.Failure -> {
                    callback.onResult(null, returnLinkResult.exception)
                    return
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
                analyticsParamRepository.didPayPalServerAttemptAppSwitch =
                    paypalPaymentResource.isAppSwitchFlow

                setAppSwitchUseCase(
                    merchantEnabledAppSwitch = payPalRequest.enablePayPalAppSwitch,
                    appSwitchFlowFromPayPalResponse = paypalPaymentResource.isAppSwitchFlow
                )

                val contextId = extractContextId(parsedRedirectUri)

                payPalRequest.riskCorrelationId?.let { clientMetadataId ->
                    buildAndReturnPaymentAuthRequest(
                        clientMetadataId = clientMetadataId,
                        contextId = contextId,
                        parsedRedirectUri = parsedRedirectUri,
                        payPalRequest = payPalRequest,
                        configuration = configuration,
                        callback = callback
                    )
                } ?: run {
                    val dataCollectorRequest = DataCollectorInternalRequest(
                        payPalRequest.hasUserLocationConsent
                    ).apply {
                        applicationGuid = dataCollector.getPayPalInstallationGUID(context)
                        clientMetadataId = contextId
                    }
                    dataCollector.getClientMetadataId(
                        context,
                        dataCollectorRequest,
                        configuration
                    ) { clientMetadataId ->
                        buildAndReturnPaymentAuthRequest(
                            clientMetadataId = clientMetadataId,
                            contextId = contextId,
                            parsedRedirectUri = parsedRedirectUri,
                            payPalRequest = payPalRequest,
                            configuration = configuration,
                            callback = callback
                        )
                    }
                }
            } catch (exception: JSONException) {
                callback.onResult(null, exception)
            }
        }
    }

    private fun buildAndReturnPaymentAuthRequest(
        clientMetadataId: String,
        contextId: String?,
        parsedRedirectUri: Uri,
        payPalRequest: PayPalRequest,
        configuration: Configuration,
        callback: PayPalInternalClientCallback
    ) {
        val returnLink: String =
            when (val returnLinkResult = getReturnLinkUseCase(parsedRedirectUri)) {
                is GetReturnLinkUseCase.ReturnLinkResult.AppLink -> returnLinkResult.appLinkReturnUri.toString()
                is GetReturnLinkUseCase.ReturnLinkResult.DeepLink -> returnLinkResult.deepLinkFallbackUrlScheme
                is GetReturnLinkUseCase.ReturnLinkResult.Failure -> {
                    callback.onResult(null, returnLinkResult.exception)
                    return
                }
            }
        val paymentAuthRequest = PayPalPaymentAuthRequestParams(
            payPalRequest = payPalRequest,
            browserSwitchOptions = null,
            clientMetadataId = clientMetadataId,
            contextId = contextId,
            successUrl = "$returnLink://onetouch/v1/success"
        )
        if (getAppSwitchUseCase()) {
            if (!contextId.isNullOrEmpty()) {
                val flowType = if (payPalRequest.isBillingAgreement()) "va" else "ecs"
                val uri = createAppSwitchUri(parsedRedirectUri, configuration.merchantId, flowType)
                paymentAuthRequest.approvalUrl = uri.toString()
            } else {
                callback.onResult(null, BraintreeException("Missing Token for PayPal App Switch."))
            }
        } else {
            paymentAuthRequest.approvalUrl = parsedRedirectUri.toString()
        }
        callback.onResult(paymentAuthRequest, null)
    }

    /**
     * Builds an app switch [Uri] with required observability parameters.
     *
     * Adds `merchant` (integration’s merchant ID) and `flow_type` (e.g. "va" or "ecs")
     * so that downstream systems can attribute sessions and distinguish checkout flows.
     * These parameters support observability for BT SDK app switch integrations.
     *
     * @param uri The base [Uri] to build upon.
     * @param flowType The checkout flow type.
     * @param merchantId The merchant ID for the integration.
     */
    private fun createAppSwitchUri(uri: Uri, merchantId: String, flowType: String): Uri {
        return uri.buildUpon()
            .appendQueryParameter("source", "braintree_sdk")
            .appendQueryParameter("switch_initiated_time", System.currentTimeMillis().toString())
            .appendQueryParameter("merchant", merchantId)
            .appendQueryParameter("flow_type", flowType)
            .build()
    }

    private fun extractContextId(redirectUri: Uri): String? {
        return redirectUri.getQueryParameter("ba_token")
            ?: redirectUri.getQueryParameter("token")
    }

    private fun PayPalRequest.isBillingAgreement(): Boolean = this is PayPalVaultRequest

    companion object {
        private const val CREATE_SINGLE_PAYMENT_ENDPOINT = "paypal_hermes/create_payment_resource"
        private const val SETUP_BILLING_AGREEMENT_ENDPOINT = "paypal_hermes/setup_billing_agreement"
    }
}
