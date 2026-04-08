package com.braintreepayments.api.paypal

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import androidx.core.net.toUri
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.LaunchType
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.BraintreeRequestCodes
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.core.LinkType
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.core.UserCanceledException
import com.braintreepayments.api.core.usecase.GetAppLinksCompatibleBrowserUseCase
import com.braintreepayments.api.core.usecase.GetDefaultAppUseCase
import com.braintreepayments.api.core.usecase.GetReturnLinkTypeUseCase
import com.braintreepayments.api.core.usecase.GetReturnLinkTypeUseCase.ReturnLinkTypeResult
import com.braintreepayments.api.core.usecase.GetReturnLinkUseCase
import com.braintreepayments.api.paypal.PayPalPaymentIntent.Companion.fromString
import com.braintreepayments.api.sharedutils.Json
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

/**
 * Used to tokenize PayPal accounts. For more information see the [documentation](https://developer.paypal.com/braintree/docs/guides/paypal/overview/android/v4)
 */
@Suppress("TooManyFunctions")
class PayPalClient internal constructor(
    private val braintreeClient: BraintreeClient,
    private val internalPayPalClient: PayPalInternalClient = PayPalInternalClient(braintreeClient),
    private val merchantRepository: MerchantRepository = MerchantRepository.instance,
    getDefaultAppUseCase: GetDefaultAppUseCase =
        GetDefaultAppUseCase(merchantRepository.applicationContext.packageManager),
    getAppLinksCompatibleBrowserUseCase: GetAppLinksCompatibleBrowserUseCase =
        GetAppLinksCompatibleBrowserUseCase(getDefaultAppUseCase),
    getReturnLinkTypeUseCase: GetReturnLinkTypeUseCase = GetReturnLinkTypeUseCase(
        merchantRepository,
        getDefaultAppUseCase,
        getAppLinksCompatibleBrowserUseCase
    ),
    private val getReturnLinkUseCase: GetReturnLinkUseCase = GetReturnLinkUseCase(merchantRepository),
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val coroutineScope: CoroutineScope = CoroutineScope(dispatcher)
) {

    /**
     * True if `tokenize()` was called with a Vault request object type
     */
    private var isVaultRequest = false

    /**
     * Used for sending Shopper Insights session ID provided by merchant to FPTI
     */
    private var shopperSessionId: String? = null

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
        coroutineScope.launch {
            val result = createPaymentAuthRequest(context, payPalRequest)
            callback.onPayPalPaymentAuthRequest(result)
        }
    }

    @OptIn(ExperimentalBetaApi::class)
    private suspend fun createPaymentAuthRequest(
        context: Context,
        payPalRequest: PayPalRequest
    ): PayPalPaymentAuthRequest {
        shopperSessionId = payPalRequest.shopperSessionId
        isVaultRequest = payPalRequest is PayPalVaultRequest
        analyticsParamRepository.didEnablePayPalAppSwitch = payPalRequest.enablePayPalAppSwitch
        val analyticsEventParams = AnalyticsEventParams(
            contextId = null,
            isVaultRequest = isVaultRequest,
            shopperSessionId = shopperSessionId
        )
        braintreeClient.sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_STARTED, analyticsEventParams)

        try {
            val configuration = braintreeClient.getConfiguration()
            if (!configuration.isPayPalEnabled) {
                val requestFailure = PayPalPaymentAuthRequest.Failure(BraintreeException(PAYPAL_NOT_ENABLED_MESSAGE))
                sendCreatePaymentAuthFailureEvent(
                    requestFailure,
                    analyticsEventParams
                )
                return requestFailure
            } else {
                return sendPayPalRequest(context, payPalRequest, configuration)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            sendCreatePaymentAuthFailureEvent(
                PayPalPaymentAuthRequest.Failure(e),
                analyticsEventParams
            )
            return PayPalPaymentAuthRequest.Failure(e)
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun sendPayPalRequest(
        context: Context,
        payPalRequest: PayPalRequest,
        configuration: Configuration
    ): PayPalPaymentAuthRequest {
        return try {
            val payPalResponse = internalPayPalClient.sendRequest(
                context,
                payPalRequest,
                configuration,
            )

            val analyticsEventParams = AnalyticsEventParams(
                contextId = payPalResponse.contextId,
                isVaultRequest = isVaultRequest,
                shopperSessionId = shopperSessionId
            )

            try {
                payPalResponse.browserSwitchOptions = buildBrowserSwitchOptions(payPalResponse)
                PayPalPaymentAuthRequest.ReadyToLaunch(payPalResponse)
            } catch (exception: Exception) {
                when (exception) {
                    is JSONException, is BraintreeException -> {
                        val failure = PayPalPaymentAuthRequest.Failure(exception)
                        sendCreatePaymentAuthFailureEvent(failure, analyticsEventParams)
                        failure
                    }
                    else -> throw exception
                }
            }
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            val failure = PayPalPaymentAuthRequest.Failure(exception)
            sendCreatePaymentAuthFailureEvent(
                failure,
                AnalyticsEventParams(
                    contextId = null,
                    isVaultRequest = isVaultRequest,
                    shopperSessionId = shopperSessionId
                )
            )
            failure
        }
    }

    @Throws(JSONException::class, BraintreeException::class)
    private fun buildBrowserSwitchOptions(
        paymentAuthRequest: PayPalPaymentAuthRequestParams
    ): BrowserSwitchOptions {
        val paymentType = if (paymentAuthRequest.isVaultRequest) {
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

        val approvalUri = Uri.parse(paymentAuthRequest.approvalUrl)

        return BrowserSwitchOptions()
            .requestCode(BraintreeRequestCodes.PAYPAL.code)
            .url(approvalUri)
            .launchType(LaunchType.ACTIVITY_CLEAR_TOP)
            .metadata(metadata)
            .apply {
                when (val returnLinkResult = getReturnLinkUseCase(approvalUri)) {
                    is GetReturnLinkUseCase.ReturnLinkResult.AppLink -> {
                        appLinkUri(returnLinkResult.appLinkReturnUri)
                        successAppLinkUri(paymentAuthRequest.successUrl?.toUri())
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
    fun tokenize(
        paymentAuthResult: PayPalPaymentAuthResult.Success,
        callback: PayPalTokenizeCallback
    ) {
        coroutineScope.launch {
            val result = tokenize(paymentAuthResult)
            callback.onPayPalResult(result)
        }
    }

    @Suppress("SwallowedException")
    private suspend fun tokenize(
        paymentAuthResult: PayPalPaymentAuthResult.Success
    ): PayPalResult {
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
        val switchInitiatedTime = approvalUrl?.toUri()?.getQueryParameter("switch_initiated_time")
        val isAppSwitchFlow = !switchInitiatedTime.isNullOrEmpty()

        val contextId = approvalUrl?.toUri()?.getQueryParameter(tokenKey)?.takeIf { it.isNotEmpty() }
        val analyticsEventParams = AnalyticsEventParams(
            contextId = contextId,
            isVaultRequest = isVaultRequest,
            shopperSessionId = shopperSessionId,
            appSwitchUrl = paymentAuthResult.browserSwitchSuccess.returnUrl.toString()
        )

        return try {
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

            val payPalAccountNonce = internalPayPalClient.tokenize(payPalAccount)
            sendTokenizeSuccessEvent(analyticsEventParams)
            PayPalResult.Success(payPalAccountNonce)
        } catch (e: UserCanceledException) {
            sendBrowserSwitchCancelEvent(isAppSwitchFlow, analyticsEventParams)
            PayPalResult.Cancel
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            sendTokenizeFailureEvent(PayPalResult.Failure(e), analyticsEventParams)
            PayPalResult.Failure(e)
        }
    }

    @Throws(
        JSONException::class,
        UserCanceledException::class,
        PayPalBrowserSwitchException::class
    )
    private fun parseUrlResponseData(
        uri: Uri,
        successUrl: String?,
        approvalUrl: String?,
        tokenKey: String
    ): JSONObject {
        val status = uri.lastPathSegment
        if (successUrl?.toUri()?.lastPathSegment != status) {
            throw UserCanceledException("User canceled PayPal.")
        }

        val requestXoToken = approvalUrl?.toUri()?.getQueryParameter(tokenKey)
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

    private fun sendCreatePaymentAuthFailureEvent(
        failure: PayPalPaymentAuthRequest.Failure,
        analyticsEventParams: AnalyticsEventParams
    ) {
        braintreeClient.sendAnalyticsEvent(
            eventName = PayPalAnalytics.TOKENIZATION_FAILED,
            params = analyticsEventParams.copy(errorDescription = failure.error.message)
        )
        analyticsParamRepository.reset()
    }

    private fun sendBrowserSwitchCancelEvent(
        isAppSwitchFlow: Boolean,
        analyticsEventParams: AnalyticsEventParams,
    ) {
        if (isAppSwitchFlow) {
            braintreeClient.sendAnalyticsEvent(PayPalAnalytics.APP_SWITCH_CANCELED, analyticsEventParams)
        } else {
            braintreeClient.sendAnalyticsEvent(PayPalAnalytics.BROWSER_LOGIN_CANCELED, analyticsEventParams)
        }
        analyticsParamRepository.reset()
    }

    private fun sendTokenizeFailureEvent(
        failure: PayPalResult.Failure,
        analyticsEventParams: AnalyticsEventParams,
    ) {
        braintreeClient.sendAnalyticsEvent(
            PayPalAnalytics.TOKENIZATION_FAILED,
            analyticsEventParams.copy(errorDescription = failure.error.message)
        )
        analyticsParamRepository.reset()
    }

    private fun sendTokenizeSuccessEvent(
        analyticsEventParams: AnalyticsEventParams,
    ) {
        braintreeClient.sendAnalyticsEvent(PayPalAnalytics.TOKENIZATION_SUCCEEDED, analyticsEventParams)
        analyticsParamRepository.reset()
    }

    companion object {
        internal const val PAYPAL_NOT_ENABLED_MESSAGE = "PayPal is not enabled. " +
            "See https://developer.paypal.com/braintree/docs/guides/paypal/overview/android/v5 " +
            "for more information."

        internal const val BROWSER_SWITCH_EXCEPTION_MESSAGE = "The response contained inconsistent data."
    }
}
