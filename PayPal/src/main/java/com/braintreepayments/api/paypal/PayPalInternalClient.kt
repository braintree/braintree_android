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
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance
) {

    suspend fun sendRequest(
        context: Context,
        payPalRequest: PayPalRequest,
        configuration: Configuration
    ): PayPalPaymentAuthRequestParams {
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
            is GetReturnLinkUseCase.ReturnLinkResult.AppLink ->
                returnLinkResult.appLinkReturnUri.toString()

            is GetReturnLinkUseCase.ReturnLinkResult.DeepLink ->
                returnLinkResult.deepLinkFallbackUrlScheme

            is GetReturnLinkUseCase.ReturnLinkResult.Failure ->
                throw returnLinkResult.exception
        }

        val appLinkParam = if (returnLinkResult is GetReturnLinkUseCase.ReturnLinkResult.AppLink) {
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

        return sendPost(
            url = url,
            requestBody = requestBody,
            payPalRequest = payPalRequest,
            context = context,
            configuration = configuration
        )
    }

    suspend fun tokenize(payPalAccount: PayPalAccount): PayPalAccountNonce {
        val tokenizationResponse = apiClient.tokenizeREST(payPalAccount)
        return PayPalAccountNonce.fromJSON(tokenizationResponse)
    }

    @Suppress("LongMethod")
    private suspend fun sendPost(
        url: String,
        requestBody: String,
        payPalRequest: PayPalRequest,
        context: Context,
        configuration: Configuration
    ): PayPalPaymentAuthRequestParams {
        val responseBody = braintreeClient.sendPOST(
            url = url,
            data = requestBody
        )

        val payPalPaymentResource = PayPalPaymentResource.fromJson(responseBody)
        val parsedRedirectUri = Uri.parse(payPalPaymentResource.redirectUrl)

        analyticsParamRepository.didPayPalServerAttemptAppSwitch = payPalPaymentResource.isAppSwitchFlow

        setAppSwitchUseCase(
            merchantEnabledAppSwitch = payPalRequest.enablePayPalAppSwitch,
            appSwitchFlowFromPayPalResponse = payPalPaymentResource.isAppSwitchFlow
        )

        val contextId = extractContextId(parsedRedirectUri)
        val clientMetadataId = payPalRequest.riskCorrelationId ?: run {
            val dataCollectorRequest = DataCollectorInternalRequest(
                payPalRequest.hasUserLocationConsent
            ).apply {
                applicationGuid = dataCollector.getPayPalInstallationGUID(context)
                clientMetadataId = contextId
            }
            dataCollector.getClientMetadataId(context, dataCollectorRequest, configuration)
        }

        val returnLink: String = when (val returnLinkResult = getReturnLinkUseCase(parsedRedirectUri)) {
            is GetReturnLinkUseCase.ReturnLinkResult.AppLink ->
                returnLinkResult.appLinkReturnUri.toString()

            is GetReturnLinkUseCase.ReturnLinkResult.DeepLink ->
                returnLinkResult.deepLinkFallbackUrlScheme

            is GetReturnLinkUseCase.ReturnLinkResult.Failure ->
                throw returnLinkResult.exception
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
                val merchantId = configuration.merchantId
                val uri = createAppSwitchUri(parsedRedirectUri, merchantId, payPalRequest)
                paymentAuthRequest.approvalUrl = uri.toString()
            } else {
                throw BraintreeException("Missing Token for PayPal App Switch.")
            }
        } else {
            paymentAuthRequest.approvalUrl = parsedRedirectUri.toString()
        }

        return paymentAuthRequest
    }

    /**
     * Builds an app switch [Uri] with required observability parameters.
     *
     * Adds `merchant` (integration’s merchant ID) and `flow_type` (e.g. "va" or "ecs")
     * so that downstream systems can attribute sessions and distinguish checkout flows.
     * These parameters support observability for BT SDK app switch integrations.
     *
     * @param uri The base [Uri] to build upon.
     * @param merchantId The merchant ID for the integration.
     * @param payPalRequest The original [PayPalRequest] associated to the request.
     */
    private fun createAppSwitchUri(
        uri: Uri,
        merchantId: String,
        payPalRequest: PayPalRequest
    ): Uri {
        val flowType = if (payPalRequest.isBillingAgreement()) "va" else "ecs"
        val fundingSource = payPalRequest.getFundingSource()

        return uri.buildUpon()
            .appendQueryParameter("source", "braintree_sdk")
            .appendQueryParameter("switch_initiated_time", System.currentTimeMillis().toString())
            .appendQueryParameter("merchant", merchantId)
            .appendQueryParameter("flow_type", flowType)
            .appendQueryParameter("funding_source", fundingSource.value)
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
