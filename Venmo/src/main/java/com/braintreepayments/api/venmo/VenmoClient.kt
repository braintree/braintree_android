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
import com.braintreepayments.api.core.GetReturnLinkTypeUseCase
import com.braintreepayments.api.core.GetReturnLinkTypeUseCase.ReturnLinkTypeResult
import com.braintreepayments.api.core.GetReturnLinkUseCase
import com.braintreepayments.api.core.LinkType
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.core.MetadataBuilder
import org.json.JSONException
import org.json.JSONObject
import java.util.Objects

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
    getReturnLinkTypeUseCase: GetReturnLinkTypeUseCase = GetReturnLinkTypeUseCase(merchantRepository),
    private val getReturnLinkUseCase: GetReturnLinkUseCase = GetReturnLinkUseCase(merchantRepository)
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
        braintreeClient.getConfiguration { configuration: Configuration?, error: Exception? ->
            if (configuration == null && error != null) {
                callbackPaymentAuthFailure(callback, VenmoPaymentAuthRequest.Failure(error))
                return@getConfiguration
            }
            val isVenmoEnabled = configuration?.isVenmoEnabled ?: false
            if (!isVenmoEnabled) {
                callbackPaymentAuthFailure(
                    callback,
                    VenmoPaymentAuthRequest.Failure(AppSwitchNotAvailableException("Venmo is not enabled"))
                )
                return@getConfiguration
            }

            // Merchants are not allowed to collect user addresses unless ECD (Enriched Customer
            // Data) is enabled on the BT Control Panel.
            val customerDataEnabled = configuration?.venmoEnrichedCustomerDataEnabled ?: false
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
                return@getConfiguration
            }

            var venmoProfileId = request.profileId
            if (TextUtils.isEmpty(venmoProfileId)) {
                venmoProfileId = configuration?.venmoMerchantId
            }

            val finalVenmoProfileId = venmoProfileId
            venmoApi.createPaymentContext(
                request, venmoProfileId
            ) { paymentContextId: String?, exception: Exception? ->
                if (exception == null) {
                    if (!paymentContextId.isNullOrEmpty()) {
                        contextId = paymentContextId
                    }
                    try {
                        createPaymentAuthRequest(
                            context, request, configuration,
                            merchantRepository.authorization, finalVenmoProfileId,
                            paymentContextId, callback
                        )
                    } catch (e: Exception) {
                        when (e) {
                            is JSONException, is BraintreeException -> {
                                callbackPaymentAuthFailure(callback, VenmoPaymentAuthRequest.Failure(e))
                            }

                            else -> throw e
                        }
                    }
                } else {
                    callbackPaymentAuthFailure(callback, VenmoPaymentAuthRequest.Failure(exception))
                }
            }
        }
    }

    @Throws(JSONException::class)
    private fun createPaymentAuthRequest(
        context: Context,
        request: VenmoRequest,
        configuration: Configuration?,
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

        val returnLinkResult = getReturnLinkUseCase()
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

        val venmoBaseURL = Uri.parse("https://venmo.com/go/checkout")
            .buildUpon()
            .appendQueryParameter("x-success", successUri)
            .appendQueryParameter("x-error", errorUri)
            .appendQueryParameter("x-cancel", cancelUri)
            .appendQueryParameter("x-source", applicationName)
            .appendQueryParameter("braintree_merchant_id", venmoProfileId)
            .appendQueryParameter("braintree_access_token", configuration?.venmoAccessToken)
            .appendQueryParameter("braintree_environment", configuration?.venmoEnvironment)
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
     * @param callback a [VenmoInternalCallback] to receive a [VenmoAccountNonce] or
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
            callbackTokenizeSuccess(deepLinkUri, callback)
        } else if (deepLinkUri.path?.contains("cancel") == true) {
            callbackTokenizeCancel(callback)
        } else if (deepLinkUri.path?.contains("error") == true) {
            callbackTokenizeFailure(
                callback,
                VenmoResult.Failure(Exception("Error returned from Venmo."))
            )
        }
    }

    @Suppress("LongMethod")
    private fun callbackTokenizeSuccess(deepLinkUri: Uri, callback: VenmoTokenizeCallback) {
        val paymentContextId = parse(deepLinkUri.toString(), "resource_id")
        val paymentMethodNonce = parse(deepLinkUri.toString(), "payment_method_nonce")
        val username = parse(deepLinkUri.toString(), "username")

        val isClientTokenAuth = (merchantRepository.authorization is ClientToken)
        if (paymentContextId != null) {

            venmoApi.createNonceFromPaymentContext(paymentContextId) { nonce: VenmoAccountNonce?, error: Exception? ->

                if (nonce != null) {
                    isVaultRequest = sharedPrefsWriter.getVenmoVaultOption(
                        merchantRepository.applicationContext
                    )
                    if (isVaultRequest && isClientTokenAuth) {
                        vaultVenmoAccountNonce(
                            nonce.string
                        ) { venmoAccountNonce: VenmoAccountNonce?, vaultError: Exception? ->
                            if (venmoAccountNonce != null) {
                                callbackSuccess(
                                    callback,
                                    VenmoResult.Success(venmoAccountNonce)
                                )
                            } else if (vaultError != null) {
                                callbackTokenizeFailure(
                                    callback,
                                    VenmoResult.Failure(vaultError)
                                )
                            }
                        }
                    } else {
                        callbackSuccess(callback, VenmoResult.Success(nonce))
                    }
                } else if (error != null) {
                    callbackTokenizeFailure(callback, VenmoResult.Failure(error))
                }
            }
        } else if (paymentMethodNonce != null && username != null) {
            isVaultRequest = sharedPrefsWriter.getVenmoVaultOption(
                merchantRepository.applicationContext
            )

            if (isVaultRequest && isClientTokenAuth) {
                vaultVenmoAccountNonce(
                    paymentMethodNonce
                ) { venmoAccountNonce: VenmoAccountNonce?, error: Exception? ->

                    if (venmoAccountNonce != null) {
                        callbackSuccess(callback, VenmoResult.Success(venmoAccountNonce))
                    } else if (error != null) {
                        callbackTokenizeFailure(callback, VenmoResult.Failure(error))
                    }
                }
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

    private fun vaultVenmoAccountNonce(nonce: String, callback: VenmoInternalCallback) {
        venmoApi.vaultVenmoAccountNonce(nonce, callback)
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
