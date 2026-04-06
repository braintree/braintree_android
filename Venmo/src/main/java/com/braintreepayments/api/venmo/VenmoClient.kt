package com.braintreepayments.api.venmo

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.util.Base64
import androidx.core.net.toUri
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.ApiClient
import com.braintreepayments.api.core.AppSwitchNotAvailableException
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.BraintreeRequestCodes
import com.braintreepayments.api.core.ClientToken
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.LinkType
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.core.MetadataBuilder
import com.braintreepayments.api.core.usecase.GetAppLinksCompatibleBrowserUseCase
import com.braintreepayments.api.core.usecase.GetDefaultAppUseCase
import com.braintreepayments.api.core.usecase.GetReturnLinkTypeUseCase
import com.braintreepayments.api.core.usecase.GetReturnLinkTypeUseCase.ReturnLinkTypeResult
import com.braintreepayments.api.core.usecase.GetReturnLinkUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.util.Objects
import kotlin.coroutines.cancellation.CancellationException

/**
 * Used to create and tokenize Venmo accounts. For more information see the [documentation](https://developer.paypal.com/braintree/docs/guides/venmo/overview)
 */
class VenmoClient internal constructor(
    private val braintreeClient: BraintreeClient,
    private val apiClient: ApiClient = ApiClient(braintreeClient),
    private val venmoApi: VenmoApi = VenmoApi(braintreeClient, apiClient),
    private val sharedPrefsWriter: VenmoSharedPrefsWriter = VenmoSharedPrefsWriter(),
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance,
    private val merchantRepository: MerchantRepository = MerchantRepository.instance,
    private val venmoRepository: VenmoRepository = VenmoRepository.instance,
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
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val coroutineScope: CoroutineScope = CoroutineScope(dispatcher)
) {
    /**
     * Used for linking events from the client to server side request
     * In the Venmo flow this will be a Payment Context ID
     */
    private var contextId: String? = null

    /**
     * True if `tokenize()` was called with a Vault request object type
     */
    private var isVaultRequest = false

    init {
        analyticsParamRepository.linkType = when (getReturnLinkTypeUseCase()) {
            ReturnLinkTypeResult.APP_LINK -> LinkType.APP_LINK
            ReturnLinkTypeResult.DEEP_LINK -> LinkType.DEEP_LINK
        }
    }

    /**
     * Initializes a new [VenmoClient] instance
     *
     * @param context an Android Context
     * @param authorization a Tokenization Key or Client Token used to authenticate
     * @param appLinkReturnUrl A [Uri] containing the Android App Link website associated with
     * your application to be used to return to your app from the PayPal
     * @param deepLinkFallbackUrlScheme A return url scheme that will be used as a deep link fallback when returning to
     * your app via App Link is not available (buyer unchecks the "Open supported links" setting).
     */
    @JvmOverloads
    constructor(
        context: Context,
        authorization: String,
        appLinkReturnUrl: Uri,
        deepLinkFallbackUrlScheme: String? = null
    ) : this(
        BraintreeClient(
            context = context,
            authorization = authorization,
            returnUrlScheme = null,
            appLinkReturnUri = appLinkReturnUrl,
            deepLinkFallbackUrlScheme = deepLinkFallbackUrlScheme
        )
    )

    /**
     * Initializes a new [VenmoClient] instance
     *
     * @param context an Android Context
     * @param authorization a Tokenization Key or Client Token used to authenticate
     * @param returnUrlScheme a custom return url to use for browser and app switching
     */
    @Deprecated("Use the constructor that uses an `appLinkReturnUrl` to redirect back to your application instead.")
    @JvmOverloads
    constructor(
        context: Context,
        authorization: String,
        returnUrlScheme: String? = null
    ) : this(
        BraintreeClient(
            context = context,
            authorization = authorization,
            deepLinkFallbackUrlScheme = returnUrlScheme
        )
    )

    /**
     * Start the Pay With Venmo flow. This will return a [VenmoPaymentAuthRequestParams] that
     * will be used to authenticate the user by switching to the Venmo app or mobile browser in
     * [VenmoLauncher.launch]
     *
     * @param context  Android Context
     * @param request  [VenmoRequest]
     * @param callback [VenmoPaymentAuthRequestCallback]
     */
    @Suppress("LongMethod", "CyclomaticComplexMethod", "TooGenericExceptionCaught")
    fun createPaymentAuthRequest(
        context: Context,
        request: VenmoRequest,
        callback: VenmoPaymentAuthRequestCallback
    ) {
        braintreeClient.sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_STARTED)
        coroutineScope.launch {
            try {
                val configuration = braintreeClient.getConfiguration()
                val isVenmoEnabled = configuration.isVenmoEnabled
                if (!isVenmoEnabled) {
                    callbackPaymentAuthFailure(
                        callback,
                        VenmoPaymentAuthRequest.Failure(AppSwitchNotAvailableException("Venmo is not enabled"))
                    )
                    return@launch
                }

                // Merchants are not allowed to collect user addresses unless ECD (Enriched Customer
                // Data) is enabled on the BT Control Panel.
                val customerDataEnabled = configuration.venmoEnrichedCustomerDataEnabled
                if ((request.collectCustomerShippingAddress ||
                            request.collectCustomerBillingAddress) && !customerDataEnabled
                ) {
                    callbackPaymentAuthFailure(
                        callback, VenmoPaymentAuthRequest.Failure(
                            BraintreeException(
                                "Cannot collect customer data when ECD is disabled. Enable this feature " +
                                        "in the Control Panel to collect this data."
                            )
                        )
                    )
                    return@launch
                }

                var venmoProfileId = request.profileId
                if (TextUtils.isEmpty(venmoProfileId)) {
                    venmoProfileId = configuration.venmoMerchantId
                }

                val paymentContextId = venmoApi.createPaymentContext(request, venmoProfileId)
                contextId = paymentContextId
                createPaymentAuthRequest(
                    context, request, configuration,
                    merchantRepository.authorization, venmoProfileId,
                    paymentContextId, callback
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                callbackPaymentAuthFailure(callback, VenmoPaymentAuthRequest.Failure(e))
            }
        }
    }

    @Throws(JSONException::class)
    private fun createPaymentAuthRequest(
        context: Context,
        request: VenmoRequest,
        configuration: Configuration,
        authorization: Authorization,
        venmoProfileId: String?,
        paymentContextId: String?,
        callback: VenmoPaymentAuthRequestCallback
    ) {
        val isClientTokenAuth = (authorization is ClientToken)
        isVaultRequest = request.shouldVault && isClientTokenAuth
        sharedPrefsWriter.persistVenmoVaultOption(context, isVaultRequest)

        val metadata = MetadataBuilder()
            .sessionId(analyticsParamRepository.sessionId)
            .integration(merchantRepository.integrationType)
            .version()
            .build()

        val braintreeData = JSONObject()
            .put("_meta", metadata)

        val applicationName = context.packageManager.getApplicationLabel(context.applicationInfo).toString()

        val venmoBaseUri = "https://venmo.com/go/checkout".toUri()

        val returnLinkResult = getReturnLinkUseCase(venmoBaseUri)
        val merchantBaseUrl: String = when (returnLinkResult) {
            is GetReturnLinkUseCase.ReturnLinkResult.AppLink -> returnLinkResult.appLinkReturnUri.toString()
            is GetReturnLinkUseCase.ReturnLinkResult.DeepLink -> {
                "${returnLinkResult.deepLinkFallbackUrlScheme}://x-callback-url/vzero/auth/venmo"
            }

            is GetReturnLinkUseCase.ReturnLinkResult.Failure -> throw returnLinkResult.exception
        }

        val successUri = "$merchantBaseUrl/success"
        val cancelUri = "$merchantBaseUrl/cancel"
        val errorUri = "$merchantBaseUrl/error"

        val venmoBaseURL = venmoBaseUri
            .buildUpon()
            .appendQueryParameter("x-success", successUri)
            .appendQueryParameter("x-error", errorUri)
            .appendQueryParameter("x-cancel", cancelUri)
            .appendQueryParameter("x-source", applicationName)
            .appendQueryParameter("braintree_merchant_id", venmoProfileId)
            .appendQueryParameter("braintree_access_token", configuration.venmoAccessToken)
            .appendQueryParameter("braintree_environment", configuration.venmoEnvironment)
            .appendQueryParameter("resource_id", paymentContextId)
            .appendQueryParameter(
                "braintree_sdk_data",
                Base64.encodeToString(braintreeData.toString().toByteArray(), 0)
            )
            .appendQueryParameter("customerClient", "MOBILE_APP")
            .build()

        venmoRepository.venmoUrl = venmoBaseURL

        val browserSwitchOptions = BrowserSwitchOptions()
            .requestCode(BraintreeRequestCodes.VENMO.code)
            .url(venmoBaseURL)
            .apply {
                when (returnLinkResult) {
                    is GetReturnLinkUseCase.ReturnLinkResult.AppLink -> {
                        appLinkUri(returnLinkResult.appLinkReturnUri)
                        successAppLinkUri(successUri.toUri())
                    }
                    is GetReturnLinkUseCase.ReturnLinkResult.DeepLink -> {
                        returnUrlScheme(returnLinkResult.deepLinkFallbackUrlScheme)
                    }

                    is GetReturnLinkUseCase.ReturnLinkResult.Failure -> throw returnLinkResult.exception
                }
            }
        val params = VenmoPaymentAuthRequestParams(browserSwitchOptions)

        callback.onVenmoPaymentAuthRequest(VenmoPaymentAuthRequest.ReadyToLaunch(params))
    }

    /**
     * After successfully authenticating a Venmo user account via [ ][VenmoClient.createPaymentAuthRequest],
     * this method should be invoked to tokenize the account to retrieve a
     * [VenmoAccountNonce].
     *
     * @param paymentAuthResult the result of [VenmoLauncher.handleReturnToApp]
     * @param callback a [VenmoTokenizeCallback] to receive a [VenmoAccountNonce] or
     * error from Venmo tokenization
     */
    fun tokenize(
        paymentAuthResult: VenmoPaymentAuthResult.Success,
        callback: VenmoTokenizeCallback
    ) {
        val browserSwitchResultInfo: BrowserSwitchFinalResult.Success =
            paymentAuthResult.browserSwitchSuccess

        val deepLinkUri: Uri = browserSwitchResultInfo.returnUrl
        braintreeClient.sendAnalyticsEvent(VenmoAnalytics.APP_SWITCH_SUCCEEDED, analyticsParams)
        if (Objects.requireNonNull(deepLinkUri.path?.contains("success")) == true) {
            coroutineScope.launch {
                tokenizeSuccess(deepLinkUri, callback)
            }
        } else if (deepLinkUri.path?.contains("cancel") == true) {
            callbackTokenizeCancel(callback)
        } else if (deepLinkUri.path?.contains("error") == true) {
            callbackTokenizeFailure(
                callback,
                VenmoResult.Failure(Exception("Error returned from Venmo."))
            )
        }
    }

    @Suppress("LongMethod", "TooGenericExceptionCaught")
    private suspend fun tokenizeSuccess(deepLinkUri: Uri, callback: VenmoTokenizeCallback) {
        val paymentContextId = parse(deepLinkUri.toString(), "resource_id")
        val paymentMethodNonce = parse(deepLinkUri.toString(), "payment_method_nonce")
        val username = parse(deepLinkUri.toString(), "username")

        val isClientTokenAuth = (merchantRepository.authorization is ClientToken)
        try {
            if (paymentContextId != null) {
                val nonce = venmoApi.createNonceFromPaymentContext(paymentContextId)
                isVaultRequest = sharedPrefsWriter.getVenmoVaultOption(
                    merchantRepository.applicationContext
                )
                if (isVaultRequest && isClientTokenAuth) {
                    val vaultedNonce = venmoApi.vaultVenmoAccountNonce(nonce.string)
                    callbackSuccess(callback, VenmoResult.Success(vaultedNonce))
                } else {
                    callbackSuccess(callback, VenmoResult.Success(nonce))
                }
            } else if (paymentMethodNonce != null && username != null) {
                isVaultRequest = sharedPrefsWriter.getVenmoVaultOption(
                    merchantRepository.applicationContext
                )

                if (isVaultRequest && isClientTokenAuth) {
                    val vaultedNonce = venmoApi.vaultVenmoAccountNonce(paymentMethodNonce)
                    callbackSuccess(callback, VenmoResult.Success(vaultedNonce))
                } else {
                    val venmoAccountNonce = VenmoAccountNonce(
                        paymentMethodNonce,
                        isDefault = false,
                        email = null,
                        externalId = null,
                        firstName = null,
                        lastName = null,
                        phoneNumber = null,
                        username,
                        billingAddress = null,
                        shippingAddress = null
                    )
                    callbackSuccess(callback, VenmoResult.Success(venmoAccountNonce))
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            callbackTokenizeFailure(callback, VenmoResult.Failure(e))
        }
    }

    private fun parse(deepLinkUri: String, key: String): String? {
        val keyFromBrowserSwitch = Uri.parse(deepLinkUri).getQueryParameter(key)
        if (keyFromBrowserSwitch != null) {
            return keyFromBrowserSwitch
        } else {
            val cleanedAppSwitchUri = deepLinkUri.replaceFirst("&".toRegex(), "?")
            return Uri.parse(cleanedAppSwitchUri).getQueryParameter(key)
        }
    }

    private fun callbackPaymentAuthFailure(
        callback: VenmoPaymentAuthRequestCallback,
        request: VenmoPaymentAuthRequest.Failure
    ) {
        braintreeClient.sendAnalyticsEvent(
            VenmoAnalytics.TOKENIZE_FAILED,
            analyticsParams.copy(errorDescription = request.error.message)
        )
        callback.onVenmoPaymentAuthRequest(request)
        analyticsParamRepository.reset()
    }

    private fun callbackSuccess(callback: VenmoTokenizeCallback, venmoResult: VenmoResult) {
        braintreeClient.sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_SUCCEEDED, analyticsParams)
        callback.onVenmoResult(venmoResult)
        analyticsParamRepository.reset()
    }

    private fun callbackTokenizeCancel(callback: VenmoTokenizeCallback) {
        braintreeClient.sendAnalyticsEvent(VenmoAnalytics.APP_SWITCH_CANCELED, analyticsParams)
        callback.onVenmoResult(VenmoResult.Cancel)
        analyticsParamRepository.reset()
    }

    private fun callbackTokenizeFailure(callback: VenmoTokenizeCallback, venmoResult: VenmoResult.Failure) {
        braintreeClient.sendAnalyticsEvent(
            VenmoAnalytics.TOKENIZE_FAILED,
            analyticsParams.copy(errorDescription = venmoResult.error.message)
        )
        callback.onVenmoResult(venmoResult)
        analyticsParamRepository.reset()
    }

    private val analyticsParams: AnalyticsEventParams
        get() {
            val eventParameters = AnalyticsEventParams(
                contextId = contextId,
                isVaultRequest = isVaultRequest,
                appSwitchUrl = venmoRepository.venmoUrl.toString(),
            )
            return eventParameters
        }
}
