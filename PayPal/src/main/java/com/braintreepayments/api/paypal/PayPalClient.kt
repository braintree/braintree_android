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
import com.braintreepayments.api.core.AppSwitchRepository
import com.braintreepayments.api.core.UserCanceledException
import com.braintreepayments.api.core.usecase.GetAppLinksCompatibleBrowserUseCase
import com.braintreepayments.api.core.usecase.GetAppSwitchUseCase
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
    private val appSwitchRepository: AppSwitchRepository = AppSwitchRepository.instance,
    private val getAppSwitchUseCase: GetAppSwitchUseCase = GetAppSwitchUseCase(appSwitchRepository),
    private val pendingPaymentStore: PendingPaymentStore = PendingPaymentStore.instance,
    private val autoLinkTokenizeUseCase: AutoLinkTokenizeUseCase =
        AutoLinkTokenizeUseCase(internalPayPalClient),
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
     * On success [callback] is called with a [PayPalPaymentAuthRequest.ReadyToLaunch] wrapping a
     * [PayPalPaymentAuthRequestParams].
     * On failures [callback] is called with a [PayPalPaymentAuthRequest.Failure] with an exception.
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
        createPaymentAuthRequest(context, payPalRequest, callback, tokenizeCallback = null)
    }

    /**
     * Starts the PayPal payment flow with auto-link re-click support.
     *
     * If a previous app switch session exists (BA approved but App Link return failed) and the
     * user taps PayPal again, the SDK will attempt to tokenize the stored BA token directly.
     * On success, the nonce is delivered via [tokenizeCallback] — skipping launch,
     * handleReturnToApp, and tokenize entirely.
     *
     * If auto-link fails or no pending session exists, the normal flow proceeds via [callback].
     *
     * @param context          Android Context
     * @param payPalRequest    a [PayPalRequest] used to customize the request
     * @param callback         [PayPalPaymentAuthCallback] for the normal flow
     * @param tokenizeCallback optional [PayPalTokenizeCallback] for auto-link re-click delivery
     */
    @OptIn(ExperimentalBetaApi::class)
    fun createPaymentAuthRequest(
        context: Context,
        payPalRequest: PayPalRequest,
        callback: PayPalPaymentAuthCallback,
        tokenizeCallback: PayPalTokenizeCallback?
    ) {
        coroutineScope.launch {
            if (tokenizeCallback != null && tryAutoLinkReclick(payPalRequest, tokenizeCallback)) {
                return@launch
            }
            val result = createPaymentAuthRequest(context, payPalRequest)
            callback.onPayPalPaymentAuthRequest(result)
        }
    }

    /**
     * Attempts auto-link tokenization when the user re-taps PayPal after a failed App Link return.
     *
     * @return true if a nonce was delivered via [tokenizeCallback] and the normal flow should be
     * skipped; false if there was no eligible pending session or auto-link failed (fall through to
     * the normal flow).
     */
    @OptIn(ExperimentalBetaApi::class)
    @Suppress("TooGenericExceptionCaught")
    private suspend fun tryAutoLinkReclick(
        payPalRequest: PayPalRequest,
        tokenizeCallback: PayPalTokenizeCallback
    ): Boolean {
        val session = pendingPaymentStore.pendingSession ?: return false
        if (session.isExpired() || !getAppSwitchUseCase()) return false

        val analyticsParams = AnalyticsEventParams(
            contextId = session.baToken,
            isVaultRequest = true,
            shopperSessionId = payPalRequest.shopperSessionId
        )
        braintreeClient.sendAnalyticsEvent(PayPalAnalytics.AUTO_LINK_RECLICK_STARTED, analyticsParams)

        return try {
            val nonce = attemptAutoLinkTokenization()
            if (nonce != null) {
                pendingPaymentStore.clear()
                braintreeClient.sendAnalyticsEvent(PayPalAnalytics.AUTO_LINK_RECLICK_SUCCEEDED, analyticsParams)
                tokenizeCallback.onPayPalResult(PayPalResult.Success(nonce))
                true
            } else {
                false
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            pendingPaymentStore.clear()
            braintreeClient.sendAnalyticsEvent(
                PayPalAnalytics.AUTO_LINK_RECLICK_FAILED,
                analyticsParams.copy(errorDescription = e.message)
            )
            false
        }
    }

    /**
     * Starts the PayPal payment flow by creating a [PayPalPaymentAuthRequestParams] to be
     * used to launch the PayPal web authentication flow in
     * [PayPalLauncher.launch].
     *
     * On success returns a [PayPalPaymentAuthRequest.ReadyToLaunch] wrapping a [PayPalPaymentAuthRequestParams].
     * On failures returns a [PayPalPaymentAuthRequest.Failure] with an exception.
     *
     * @param context       Android Context
     * @param payPalRequest a [PayPalRequest] used to customize the request.
     * @return [PayPalPaymentAuthRequest]
     */
    @OptIn(ExperimentalBetaApi::class)
    suspend fun createPaymentAuthRequest(
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
                storePendingSessionIfEligible(payPalResponse)
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
     * [PayPalLauncher.handleReturnToApp], pass the resulting [PayPalPaymentAuthResult.Success]
     * to this method to tokenize the PayPal account and receive a [PayPalResult.Success] containing
     * a [PayPalAccountNonce] on success.
     *
     * @param paymentAuthResult a [PayPalPaymentAuthResult.Success] received in the callback
     * from  [PayPalLauncher.handleReturnToApp]
     * @param callback [PayPalTokenizeCallback]
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

    /**
     * After receiving a result from the PayPal web authentication flow via
     * [PayPalLauncher.handleReturnToApp], pass the resulting [PayPalPaymentAuthResult.Success]
     * to this method to tokenize the PayPal account and receive a [PayPalResult.Success] containing
     * a [PayPalAccountNonce] on success.
     *
     * @param paymentAuthResult a [PayPalPaymentAuthResult.Success] received in the callback
     * from  [PayPalLauncher.handleReturnToApp]
     * @return [PayPalResult]
     */
    @Suppress("SwallowedException")
    suspend fun tokenize(
        paymentAuthResult: PayPalPaymentAuthResult.Success
    ): PayPalResult {
        // Auto-link path: no URL return — tokenize the stored BA token directly with BTGW.
        if (paymentAuthResult.autoLinkPending) {
            return tokenizeAutoLink()
        }

        val browserSwitchResult = paymentAuthResult.browserSwitchSuccess
            ?: return PayPalResult.Failure(BraintreeException("Missing browser switch result"))
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
            appSwitchUrl = browserSwitchResult.returnUrl.toString()
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
        } catch (_: UserCanceledException) {
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

    /**
     * Stores a [PendingPaymentStore.PendingSession] when the current request is an app switch
     * vault flow. The stored session enables auto-link tokenization if the App Link return fails.
     */
    private fun storePendingSessionIfEligible(payPalResponse: PayPalPaymentAuthRequestParams) {
        if (getAppSwitchUseCase() && payPalResponse.isVaultRequest) {
            val baToken = payPalResponse.approvalUrl?.toUri()?.getQueryParameter("ba_token")
            if (baToken != null) {
                pendingPaymentStore.pendingSession = PendingPaymentStore.PendingSession(
                    baToken = baToken,
                    clientMetadataId = payPalResponse.clientMetadataId,
                    merchantAccountId = payPalResponse.merchantAccountId,
                    intent = payPalResponse.intent?.stringValue,
                    paymentType = "billing-agreement"
                )
            }
        }
    }

    /**
     * Tokenizes a pending billing agreement session directly with BTGW (auto-link).
     * Invoked from [tokenize] when [PayPalPaymentAuthResult.Success.autoLinkPending] is true.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun tokenizeAutoLink(): PayPalResult {
        val analyticsEventParams = AnalyticsEventParams(
            isVaultRequest = true,
            shopperSessionId = shopperSessionId
        )
        return try {
            val nonce = attemptAutoLinkTokenization()
            if (nonce != null) {
                pendingPaymentStore.clear()
                sendTokenizeSuccessEvent(analyticsEventParams)
                PayPalResult.Success(nonce)
            } else {
                pendingPaymentStore.clear()
                PayPalResult.Failure(BraintreeException(AUTO_LINK_EXPIRED_MESSAGE))
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            pendingPaymentStore.clear()
            sendTokenizeFailureEvent(PayPalResult.Failure(e), analyticsEventParams)
            PayPalResult.Failure(e)
        }
    }

    /**
     * Attempts to tokenize a stored BA token directly with BTGW via [AutoLinkTokenizeUseCase].
     *
     * Uses [PendingPaymentStore.getOrCreateDeferred] to ensure exactly one BTGW call:
     * - If this caller is the initiator, it makes the call and completes the deferred.
     * - If another entry point already started the call, this caller awaits the same deferred.
     *
     * @return the resolved [PayPalAccountNonce], or null if the session is missing or expired.
     * @throws Exception if the BTGW tokenization call fails.
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun attemptAutoLinkTokenization(): PayPalAccountNonce? {
        val session = pendingPaymentStore.pendingSession ?: return null
        val analyticsParams = AnalyticsEventParams(
            contextId = session.baToken,
            isVaultRequest = true,
            shopperSessionId = shopperSessionId
        )
        if (session.isExpired()) {
            pendingPaymentStore.clear()
            braintreeClient.sendAnalyticsEvent(PayPalAnalytics.AUTO_LINK_EXPIRED, analyticsParams)
            return null
        }

        val (deferred, isInitiator) = pendingPaymentStore.getOrCreateDeferred()
        return if (isInitiator) {
            braintreeClient.sendAnalyticsEvent(PayPalAnalytics.AUTO_LINK_TOKENIZE_STARTED, analyticsParams)
            try {
                val nonce = autoLinkTokenizeUseCase(session)
                pendingPaymentStore.autoLinkNonce = nonce
                deferred.complete(nonce)
                braintreeClient.sendAnalyticsEvent(PayPalAnalytics.AUTO_LINK_TOKENIZE_SUCCEEDED, analyticsParams)
                nonce
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                deferred.completeExceptionally(e)
                pendingPaymentStore.autoLinkNonce = null
                pendingPaymentStore.tokenizeDeferred = null
                braintreeClient.sendAnalyticsEvent(
                    PayPalAnalytics.AUTO_LINK_TOKENIZE_FAILED,
                    analyticsParams.copy(errorDescription = e.message)
                )
                throw e
            }
        } else {
            val nonce = deferred.await()
            pendingPaymentStore.autoLinkNonce = nonce
            nonce
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

        internal const val AUTO_LINK_EXPIRED_MESSAGE =
            "The billing agreement session has expired. Please restart the PayPal flow."
    }
}
