package com.braintreepayments.api.paypal

import android.content.Context
import android.net.Uri
import com.braintreepayments.api.core.ApiClient
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.DeviceInspector
import com.braintreepayments.api.core.GetReturnLinkUseCase
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.datacollector.DataCollector
import com.braintreepayments.api.datacollector.DataCollectorInternalRequest
import com.braintreepayments.api.paypal.PayPalPaymentResource.Companion.fromJson
import org.json.JSONException
import org.json.JSONObject

internal class PayPalInternalClient(
    private val braintreeClient: BraintreeClient,
    private val dataCollector: DataCollector = DataCollector(braintreeClient),
    private val apiClient: ApiClient = ApiClient(braintreeClient),
    private val deviceInspector: DeviceInspector = DeviceInspector(),
    private val merchantRepository: MerchantRepository = MerchantRepository.instance,
    private val getReturnLinkUseCase: GetReturnLinkUseCase = GetReturnLinkUseCase(merchantRepository),
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
                payPalSetPaymentTokenUseCase.setPaymentToken(pairingId)
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
                    pairingId = pairingId,
                    successUrl = "$returnLink://onetouch/v1/success"
                )

                if (isAppSwitchEnabled(payPalRequest) && isPayPalInstalled(context)) {
                    if (!pairingId.isNullOrEmpty()) {
                        paymentAuthRequest.approvalUrl =
                            createAppSwitchUri(parsedRedirectUri).toString()
                    } else {
                        callback.onResult(
                            null,
                            BraintreeException("Missing Token for PayPal App Switch.")
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

    private fun findPairingId(redirectUri: Uri): String? {
        return redirectUri.getQueryParameter("ba_token")
            ?: redirectUri.getQueryParameter("token")
    }

    companion object {
        private const val CREATE_SINGLE_PAYMENT_ENDPOINT = "paypal_hermes/create_payment_resource"
        private const val SETUP_BILLING_AGREEMENT_ENDPOINT = "paypal_hermes/setup_billing_agreement"
    }
}
