package com.braintreepayments.api.venmo

import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.util.Base64
import androidx.annotation.VisibleForTesting
import com.braintreepayments.api.BrowserSwitchFinalResult
import com.braintreepayments.api.BrowserSwitchOptions
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.ApiClient
import com.braintreepayments.api.core.AppSwitchNotAvailableException
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.BraintreeRequestCodes
import com.braintreepayments.api.core.ClientToken
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.MetadataBuilder
import org.json.JSONException
import org.json.JSONObject
import java.util.Objects

/**
 * Used to create and tokenize Venmo accounts. For more information see the [documentation](https://developer.paypal.com/braintree/docs/guides/venmo/overview)
 */
class VenmoClient @VisibleForTesting internal constructor(
    private val braintreeClient: BraintreeClient,
    private val apiClient: ApiClient = ApiClient(braintreeClient),
    private val venmoApi: VenmoApi = VenmoApi(braintreeClient, apiClient),
    private val sharedPrefsWriter: VenmoSharedPrefsWriter = VenmoSharedPrefsWriter(),
) {
    /**
     * Used for linking events from the client to server side request
     * In the Venmo flow this will be a Payment Context ID
     */
    private var payPalContextId: String? = null

    /**
     * True if `tokenize()` was called with a Vault request object type
     */
    private var isVaultRequest = false

    /**
     * Initializes a new [VenmoClient] instance
     *
     * @param context an Android Context
     * @param authorization a Tokenization Key or Client Token used to authenticate
     * @param returnUrlScheme a custom return url to use for browser and app switching
     */
    constructor(
        context: Context,
        authorization: String,
        returnUrlScheme: String?
    ) : this(BraintreeClient(context, authorization, returnUrlScheme))

    /**
     * Start the Pay With Venmo flow. This will return a [VenmoPaymentAuthRequestParams] that
     * will be used to authenticate the user by switching to the Venmo app or mobile browser in
     * [VenmoLauncher.launch]
     *
     * @param context  Android Context
     * @param request  [VenmoRequest]
     * @param callback [VenmoPaymentAuthRequestCallback]
     */
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
                        payPalContextId = paymentContextId
                    }
                    try {
                        createPaymentAuthRequest(
                            context, request, configuration,
                            braintreeClient.authorization, finalVenmoProfileId,
                            paymentContextId, callback
                        )
                    } catch (e: JSONException) {
                        callbackPaymentAuthFailure(callback, VenmoPaymentAuthRequest.Failure(e))
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
            .sessionId(braintreeClient.sessionId)
            .integration(braintreeClient.integrationType)
            .version()
            .build()

        val braintreeData = JSONObject()
            .put("_meta", metadata)

        val applicationName =
            context.packageManager.getApplicationLabel(context.applicationInfo)
                .toString()

        val returnUrlScheme = braintreeClient.getReturnUrlScheme()
        val venmoBaseURL = Uri.parse("https://venmo.com/go/checkout")
            .buildUpon()
            .appendQueryParameter(
                "x-success", "$returnUrlScheme://x-callback-url/vzero/auth/venmo/success"
            )
            .appendQueryParameter(
                "x-error", "$returnUrlScheme://x-callback-url/vzero/auth/venmo/error"
            )
            .appendQueryParameter(
                "x-cancel", "$returnUrlScheme://x-callback-url/vzero/auth/venmo/cancel"
            )
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

        val browserSwitchOptions = BrowserSwitchOptions()
            .requestCode(BraintreeRequestCodes.VENMO.code)
            .url(venmoBaseURL)
            .returnUrlScheme(returnUrlScheme)
        val params = VenmoPaymentAuthRequestParams(
            browserSwitchOptions
        )

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
        val venmoPaymentAuthResultInfo = paymentAuthResult.paymentAuthInfo
        val browserSwitchResultInfo: BrowserSwitchFinalResult.Success =
            venmoPaymentAuthResultInfo.browserSwitchResultInfo

        val deepLinkUri: Uri = browserSwitchResultInfo.returnUrl
        if (deepLinkUri != null) {
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
        } else {
            callbackTokenizeFailure(callback, VenmoResult.Failure(Exception("Unknown error")))
        }
    }

    private fun callbackTokenizeSuccess(deepLinkUri: Uri, callback: VenmoTokenizeCallback) {
        val paymentContextId = parse(deepLinkUri.toString(), "resource_id")
        val paymentMethodNonce = parse(deepLinkUri.toString(), "payment_method_nonce")
        val username = parse(deepLinkUri.toString(), "username")

        val isClientTokenAuth = (braintreeClient.authorization is ClientToken)
        if (paymentContextId != null) {

            venmoApi.createNonceFromPaymentContext(paymentContextId) { nonce: VenmoAccountNonce?, error: Exception? ->

                if (nonce != null) {
                    isVaultRequest = sharedPrefsWriter.getVenmoVaultOption(
                        braintreeClient.applicationContext
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
                braintreeClient.applicationContext
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
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    username,
                    null,
                    null
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
        request: VenmoPaymentAuthRequest
    ) {
        braintreeClient.sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_FAILED, analyticsParams)
        callback.onVenmoPaymentAuthRequest(request)
    }

    private fun callbackSuccess(callback: VenmoTokenizeCallback, venmoResult: VenmoResult) {
        braintreeClient.sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_SUCCEEDED, analyticsParams)
        callback.onVenmoResult(venmoResult)
    }

    private fun callbackTokenizeCancel(callback: VenmoTokenizeCallback) {
        braintreeClient.sendAnalyticsEvent(VenmoAnalytics.APP_SWITCH_CANCELED, analyticsParams)
        callback.onVenmoResult(VenmoResult.Cancel)
    }

    private fun callbackTokenizeFailure(callback: VenmoTokenizeCallback, venmoResult: VenmoResult) {
        braintreeClient.sendAnalyticsEvent(VenmoAnalytics.TOKENIZE_FAILED, analyticsParams)
        callback.onVenmoResult(venmoResult)
    }

    private val analyticsParams: AnalyticsEventParams
        get() {
            val eventParameters = AnalyticsEventParams()
            eventParameters.payPalContextId = payPalContextId
            eventParameters.linkType = LINK_TYPE
            eventParameters.isVaultRequest = isVaultRequest
            return eventParameters
        }

    companion object {
        private const val LINK_TYPE = "universal"
    }
}
