package com.braintreepayments.api.paypal

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.BraintreeRequestCodes
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.core.GetReturnLinkTypeUseCase
import com.braintreepayments.api.core.GetReturnLinkTypeUseCase.ReturnLinkTypeResult
import com.braintreepayments.api.core.GetReturnLinkUseCase
import com.braintreepayments.api.core.LinkType
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.core.UserCanceledException
import com.braintreepayments.api.paypal.PayPalPaymentIntent.Companion.fromString
import com.braintreepayments.api.sharedutils.Json
import org.json.JSONException
import org.json.JSONObject

/**
 * Used to tokenize PayPal accounts. For more information see the [documentation](https://developer.paypal.com/braintree/docs/guides/paypal/overview/android/v4)
 */
class PayPalClient internal constructor(
    private val braintreeClient: BraintreeClient,
    private val internalPayPalClient: PayPalInternalClient = PayPalInternalClient(braintreeClient),
    private val merchantRepository: MerchantRepository = MerchantRepository.instance,
    getReturnLinkTypeUseCase: GetReturnLinkTypeUseCase = GetReturnLinkTypeUseCase(merchantRepository),
    private val getReturnLinkUseCase: GetReturnLinkUseCase = GetReturnLinkUseCase(merchantRepository),
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance
) {
    /**
     * Used for linking events from the client to server side request
     * In the PayPal flow this will be either an EC token or a Billing Agreement token
     */
    private var payPalContextId: String? = null

    /**
     * True if `tokenize()` was called with a Vault request object type
     */
    private var isVaultRequest = false

    /**
     * Used for sending Shopper Insights session ID provided by merchant to FPTI
     */
    private var shopperSessionId: String? = null

    /**
     * The final URL string used to open the app switch flow
     */
    private var appSwitchUrlString: String? = null

    /**
     * Initializes a new [PayPalClient] instance
     *
     * @param context          an Android Context
     * @param authorization    a Tokenization Key or Client Token used to authenticate
     * @param appLinkReturnUrl A [Uri] containing the Android App Link website associated with
     * your application to be used to return to your app from the PayPal payment flows.
     * @param deepLinkFallbackUrlScheme A return url scheme that will be used as a deep link fallback when returning to
     * your app via App Link is not available (buyer unchecks the "Open supported links" setting).
     */
    constructor(
        context: Context,
        authorization: String,
        appLinkReturnUrl: Uri,
        deepLinkFallbackUrlScheme: String? = null
    ) : this(
        BraintreeClient(
            context = context,
            authorization = authorization,
            deepLinkFallbackUrlScheme = deepLinkFallbackUrlScheme,
            appLinkReturnUri = appLinkReturnUrl
        )
    )

    init {
        analyticsParamRepository.linkType = when (getReturnLinkTypeUseCase()) {
            ReturnLinkTypeResult.APP_LINK -> LinkType.APP_LINK
            ReturnLinkTypeResult.DEEP_LINK -> LinkType.DEEP_LINK
        }
    }

    /**
     * Starts the PayPal payment flow by creating a [PayPalPaymentAuthRequestParams] to be
     * used to launch the PayPal web authentication flow in
     * [PayPalLauncher.launch].
     *
     * @param context       Android Context
     * @param payPalRequest a [PayPalRequest] used to customize the request.
     * @param callback      [PayPalPaymentAuthCallback]
     */
    @OptIn(ExperimentalBetaApi::class)
    fun createPaymentAuthRequest(
        context: Context,
        payPalRequest: PayPalRequest,
        callback: PayPalPaymentAuthCallback
    ) {
        shopperSessionId = payPalRequest.shopperSessionId
        isVaultRequest = payPalRequest is PayPalVaultRequest
        analyticsParamRepository.didEnablePayPalAppSwitch = payPalRequest.enablePayPalAppSwitch

        braintreeClient.sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_STARTED, analyticsParams)

        braintreeClient.getConfiguration { configuration: Configuration?, error: Exception? ->
            if (error != null) {
                callbackCreatePaymentAuthFailure(callback, PayPalPaymentAuthRequest.Failure(error))
            } else if (payPalConfigInvalid(configuration)) {
                callbackCreatePaymentAuthFailure(
                    callback,
                    PayPalPaymentAuthRequest.Failure(createPayPalError())
                )
            } else {
                sendPayPalRequest(context, payPalRequest, callback)
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun sendPayPalRequest(
        context: Context,
        payPalRequest: PayPalRequest,
        callback: PayPalPaymentAuthCallback
    ) {
        internalPayPalClient.sendRequest(
            context,
            payPalRequest
        ) { payPalResponse: PayPalPaymentAuthRequestParams?,
            error: Exception? ->
            if (payPalResponse != null) {
                payPalContextId = payPalResponse.paypalContextId

                try {
                    payPalResponse.browserSwitchOptions = buildBrowserSwitchOptions(payPalResponse)
                    callback.onPayPalPaymentAuthRequest(PayPalPaymentAuthRequest.ReadyToLaunch(payPalResponse))
                } catch (exception: Exception) {
                    when (exception) {
                        is JSONException,
                        is BraintreeException -> {
                            callbackCreatePaymentAuthFailure(callback, PayPalPaymentAuthRequest.Failure(exception))
                        }

                        else -> throw exception
                    }
                }
            } else {
                callbackCreatePaymentAuthFailure(
                    callback,
                    PayPalPaymentAuthRequest.Failure(error ?: BraintreeException("Error is null"))
                )
            }
        }
    }

    @Throws(JSONException::class, BraintreeException::class)
    private fun buildBrowserSwitchOptions(
        paymentAuthRequest: PayPalPaymentAuthRequestParams
    ): BrowserSwitchOptions {
        val paymentType = if (paymentAuthRequest.isBillingAgreement) {
            "billing-agreement"
        } else {
            "single-payment"
        }

        val metadata = JSONObject().apply {
            put("approval-url", paymentAuthRequest.approvalUrl)
            put("success-url", paymentAuthRequest.successUrl)
            put("payment-type", paymentType)
            put("client-metadata-id", paymentAuthRequest.clientMetadataId)
            put("merchant-account-id", paymentAuthRequest.merchantAccountId)
            put("source", "paypal-browser")
            put("intent", paymentAuthRequest.intent)
        }

        return BrowserSwitchOptions()
            .requestCode(BraintreeRequestCodes.PAYPAL.code)
            .url(Uri.parse(paymentAuthRequest.approvalUrl))
            .launchAsNewTask(braintreeClient.launchesBrowserSwitchAsNewTask())
            .metadata(metadata)
            .apply {
                when (val returnLinkResult = getReturnLinkUseCase()) {
                    is GetReturnLinkUseCase.ReturnLinkResult.AppLink -> {
                        appLinkUri(returnLinkResult.appLinkReturnUri)
                    }

                    is GetReturnLinkUseCase.ReturnLinkResult.DeepLink -> {
                        returnUrlScheme(returnLinkResult.deepLinkFallbackUrlScheme)
                    }

                    is GetReturnLinkUseCase.ReturnLinkResult.Failure -> throw returnLinkResult.exception
                }
            }
    }

    /**
     * After receiving a result from the PayPal web authentication flow via
     * [PayPalLauncher.handleReturnToApp],
     * pass the [PayPalPaymentAuthResult.Success] returned to this method to tokenize the PayPal
     * account and receive a [PayPalAccountNonce] on success.
     *
     * @param paymentAuthResult a [PayPalPaymentAuthResult.Success] received in the callback
     * from  [PayPalLauncher.handleReturnToApp]
     * @param callback          [PayPalTokenizeCallback]
     */
    @Suppress("SwallowedException")
    fun tokenize(
        paymentAuthResult: PayPalPaymentAuthResult.Success,
        callback: PayPalTokenizeCallback
    ) {
        val browserSwitchResult = paymentAuthResult.browserSwitchSuccess
        val metadata = browserSwitchResult.requestMetadata
        val clientMetadataId = Json.optString(metadata, "client-metadata-id", null)
        val merchantAccountId = Json.optString(metadata, "merchant-account-id", null)
        val payPalIntent = fromString(Json.optString(metadata, "intent", null))
        val approvalUrl = Json.optString(metadata, "approval-url", null)
        val successUrl = Json.optString(metadata, "success-url", null)
        val paymentType = Json.optString(metadata, "payment-type", "unknown")
        val isBillingAgreement = paymentType.equals("billing-agreement", ignoreCase = true)
        val tokenKey = if (isBillingAgreement) "ba_token" else "token"
        val switchInitiatedTime = Uri.parse(approvalUrl).getQueryParameter("switch_initiated_time")
        val isAppSwitchFlow = !switchInitiatedTime.isNullOrEmpty()

        if (isAppSwitchFlow) {
            appSwitchUrlString = approvalUrl
        }

        approvalUrl?.let {
            val paypalContextId = Uri.parse(approvalUrl).getQueryParameter(tokenKey)
            if (!paypalContextId.isNullOrEmpty()) {
                payPalContextId = paypalContextId
            }
        }

        try {
            val urlResponseData = parseUrlResponseData(
                uri = browserSwitchResult.returnUrl,
                successUrl = successUrl,
                approvalUrl = approvalUrl,
                tokenKey = tokenKey
            )

            val payPalAccount = PayPalAccount(
                clientMetadataId,
                urlResponseData,
                payPalIntent,
                merchantAccountId,
                paymentType
            )

            internalPayPalClient.tokenize(payPalAccount) { payPalAccountNonce: PayPalAccountNonce?, error: Exception? ->
                if (payPalAccountNonce != null) {
                    callbackTokenizeSuccess(
                        callback,
                        PayPalResult.Success(payPalAccountNonce),
                    )
                } else if (error != null) {
                    callbackTokenizeFailure(callback, PayPalResult.Failure(error))
                }
            }
        } catch (e: UserCanceledException) {
            callbackBrowserSwitchCancel(callback, PayPalResult.Cancel, isAppSwitchFlow)
        } catch (e: JSONException) {
            callbackTokenizeFailure(callback, PayPalResult.Failure(e))
        } catch (e: PayPalBrowserSwitchException) {
            callbackTokenizeFailure(callback, PayPalResult.Failure(e))
        }
    }

    @Throws(
        JSONException::class,
        UserCanceledException::class,
        PayPalBrowserSwitchException::class
    )
    private fun parseUrlResponseData(
        uri: Uri,
        successUrl: String,
        approvalUrl: String?,
        tokenKey: String
    ): JSONObject {
        val status = uri.lastPathSegment
        if (Uri.parse(successUrl).lastPathSegment != status) {
            throw UserCanceledException("User canceled PayPal.")
        }

        val requestXoToken = Uri.parse(approvalUrl).getQueryParameter(tokenKey)
        val responseXoToken = uri.getQueryParameter(tokenKey)
        if (TextUtils.equals(requestXoToken, responseXoToken)) {
            val client = JSONObject().apply {
                put("environment", null)
            }

            val response = JSONObject().apply {
                put("webURL", uri.toString())
            }

            val urlResponseData = JSONObject().apply {
                put("client", client)
                put("response", response)
                put("response_type", "web")
            }
            return urlResponseData
        } else {
            throw PayPalBrowserSwitchException(BROWSER_SWITCH_EXCEPTION_MESSAGE)
        }
    }

    private fun callbackCreatePaymentAuthFailure(
        callback: PayPalPaymentAuthCallback,
        failure: PayPalPaymentAuthRequest.Failure
    ) {
        braintreeClient.sendAnalyticsEvent(
            PayPalAnalytics.TOKENIZATION_FAILED,
            analyticsParams.copy(errorDescription = failure.error.message)
        )
        callback.onPayPalPaymentAuthRequest(failure)
        analyticsParamRepository.reset()
    }

    private fun callbackBrowserSwitchCancel(
        callback: PayPalTokenizeCallback,
        cancel: PayPalResult.Cancel,
        isAppSwitchFlow: Boolean
    ) {
        braintreeClient.sendAnalyticsEvent(PayPalAnalytics.BROWSER_LOGIN_CANCELED, analyticsParams)

        if (isAppSwitchFlow) {
            braintreeClient.sendAnalyticsEvent(PayPalAnalytics.APP_SWITCH_CANCELED, analyticsParams)
        }

        callback.onPayPalResult(cancel)
        analyticsParamRepository.reset()
    }

    private fun callbackTokenizeFailure(
        callback: PayPalTokenizeCallback,
        failure: PayPalResult.Failure,
    ) {
        braintreeClient.sendAnalyticsEvent(
            PayPalAnalytics.TOKENIZATION_FAILED,
            analyticsParams.copy(errorDescription = failure.error.message)
        )
        callback.onPayPalResult(failure)
        analyticsParamRepository.reset()
    }

    private fun callbackTokenizeSuccess(
        callback: PayPalTokenizeCallback,
        success: PayPalResult.Success,
    ) {
        braintreeClient.sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_SUCCEEDED, analyticsParams)
        callback.onPayPalResult(success)
        analyticsParamRepository.reset()
    }

    private val analyticsParams: AnalyticsEventParams
        get() {
            return AnalyticsEventParams(
                payPalContextId = payPalContextId,
                isVaultRequest = isVaultRequest,
                shopperSessionId = shopperSessionId
            )
        }

    private fun payPalConfigInvalid(configuration: Configuration?): Boolean {
        return (configuration == null || !configuration.isPayPalEnabled)
    }

    companion object {
        internal const val PAYPAL_NOT_ENABLED_MESSAGE = "PayPal is not enabled. " +
            "See https://developer.paypal.com/braintree/docs/guides/paypal/overview/android/v5 " +
            "for more information."

        internal const val BROWSER_SWITCH_EXCEPTION_MESSAGE = "The response contained inconsistent data."
        private fun createPayPalError(): Exception {
            return BraintreeException(PAYPAL_NOT_ENABLED_MESSAGE)
        }
    }
}
