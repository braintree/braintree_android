package com.braintreepayments.api.googlepay

import android.content.Context
import android.text.TextUtils
import androidx.annotation.VisibleForTesting
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.ErrorWithResponse.Companion.fromJson
import com.braintreepayments.api.core.MetadataBuilder
import com.braintreepayments.api.core.TokenizationKey
import com.braintreepayments.api.core.UserCanceledException
import com.braintreepayments.api.googlepay.GooglePayCardNonce.Companion.fromJSON
import com.braintreepayments.api.googlepay.GooglePayReadinessResult.NotReadyToPay
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.WalletConstants
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Used to create and tokenize Google Pay payment methods. For more information see the [documentation](https://developer.paypal.com/braintree/docs/guides/google-pay/overview)
 */
@SuppressWarnings("TooManyFunctions")
class GooglePayClient @VisibleForTesting internal constructor(
    private val braintreeClient: BraintreeClient,
    private val internalGooglePayClient: GooglePayInternalClient = GooglePayInternalClient(),
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance
) {
    /**
     * Initializes a new [GooglePayClient] instance
     *
     * @param context an Android Context
     * @param authorization a Tokenization Key or Client Token used to authenticate
     */
    constructor(context: Context, authorization: String) : this(
        BraintreeClient(
            context,
            authorization
        )
    )

    /**
     * Before starting the Google Pay flow, use this method to check whether the Google Pay API is
     * supported and set up on the device. When the callback is called with `true`, show the
     * Google Pay button. When it is called with `false`, display other checkout options.
     *
     * @param context  Android Context
     * @param callback [GooglePayIsReadyToPayCallback]
     */
    fun isReadyToPay(
        context: Context,
        callback: GooglePayIsReadyToPayCallback
    ) {
        isReadyToPay(context, null, callback)
    }

    /**
     * Before starting the Google Pay flow, use this method to check whether the Google Pay API is
     * supported and set up on the device. When the callback is called with `true`, show the
     * Google Pay button. When it is called with `false`, display other checkout options.
     *
     * @param context  Android Context
     * @param request  [ReadyForGooglePayRequest]
     * @param callback [GooglePayIsReadyToPayCallback]
     */
    @SuppressWarnings("SwallowedException")
    fun isReadyToPay(
        context: Context,
        request: ReadyForGooglePayRequest?,
        callback: GooglePayIsReadyToPayCallback
    ) {
        try {
            Class.forName(PaymentsClient::class.java.name)
        } catch (e: ClassNotFoundException) {
            callback.onGooglePayReadinessResult(NotReadyToPay(null))
            return
        } catch (e: NoClassDefFoundError) {
            callback.onGooglePayReadinessResult(NotReadyToPay(null))
            return
        }

        braintreeClient.getConfiguration { configuration: Configuration?, e: Exception? ->
            if (configuration == null) {
                callback.onGooglePayReadinessResult(NotReadyToPay(e))
                return@getConfiguration
            }
            if (!configuration.isGooglePayEnabled) {
                callback.onGooglePayReadinessResult(NotReadyToPay(null))
                return@getConfiguration
            }

            val json = JSONObject()
            val allowedCardNetworks = buildCardNetworks(configuration)

            try {
                json.put("apiVersion", 2).put("apiVersionMinor", 0).put(
                    "allowedPaymentMethods",
                    JSONArray().put(
                        JSONObject().put("type", "CARD")
                            .put(
                                "parameters", JSONObject().put(
                                    "allowedAuthMethods",
                                    JSONArray().put("PAN_ONLY").put("CRYPTOGRAM_3DS")
                                )
                                    .put("allowedCardNetworks", allowedCardNetworks)
                            )
                    )
                )

                if (request != null) {
                    json.put(
                        "existingPaymentMethodRequired",
                        request.isExistingPaymentMethodRequired
                    )
                }
            } catch (ignored: JSONException) {
            }
            val readyToPayRequest = IsReadyToPayRequest.fromJson(json.toString())
            internalGooglePayClient.isReadyToPay(context, configuration, readyToPayRequest, callback)
        }
    }

    /**
     * Get Braintree specific tokenization parameters for a Google Pay. Useful for when full control
     * over the [PaymentDataRequest] is required.
     *
     *
     * [PaymentMethodTokenizationParameters] should be supplied to the
     * [PaymentDataRequest] via
     * [ ][PaymentDataRequest.Builder.setPaymentMethodTokenizationParameters]
     * and [&lt;Integer&gt;][Collection] allowedCardNetworks should be supplied to the
     * [CardRequirements] via
     * [CardRequirements.Builder.addAllowedCardNetworks]}.
     *
     * @param callback [GooglePayGetTokenizationParametersCallback]
     */
    fun getTokenizationParameters(
        callback: GooglePayGetTokenizationParametersCallback
    ) {
        braintreeClient.getConfiguration { configuration: Configuration?, e: Exception? ->

            if (configuration != null) {
                callback.onTokenizationParametersResult(
                    GooglePayTokenizationParameters.Success(
                        getTokenizationParameters(configuration, braintreeClient.authorization),
                        getAllowedCardNetworks(configuration)
                    )
                )
            } else {
                if (e != null) {
                    callback.onTokenizationParametersResult(GooglePayTokenizationParameters.Failure(e))
                } else {
                    callback.onTokenizationParametersResult(null)
                }
            }
        }
    }

    /**
     * Start the Google Pay payment flow. This will return [GooglePayPaymentAuthRequestParams] that are
     * used to present Google Pay payment sheet in
     * [GooglePayLauncher.launch]
     *
     * @param request  The [GooglePayRequest] containing options for the transaction.
     * @param callback [GooglePayPaymentAuthRequestCallback]
     */
    @SuppressWarnings("LongMethod")
    fun createPaymentAuthRequest(
        request: GooglePayRequest,
        callback: GooglePayPaymentAuthRequestCallback
    ) {
        analyticsParamRepository.resetSessionId()
        braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.PAYMENT_REQUEST_STARTED)

        if (!validateManifest()) {
            callbackPaymentRequestFailure(
                GooglePayPaymentAuthRequest.Failure(
                    BraintreeException(
                        "GooglePayActivity was not found in the Android " +
                                "manifest, or did not have a theme of R.style.bt_transparent_activity"
                    )
                ), callback
            )
            return
        }

        if (request.transactionInfo == null) {
            callbackPaymentRequestFailure(
                GooglePayPaymentAuthRequest.Failure(
                    BraintreeException(
                        "Cannot pass null TransactionInfo to requestPayment"
                    )
                ), callback
            )
            return
        }

        braintreeClient.getConfiguration { configuration: Configuration?, configError: Exception? ->

            if (configuration?.isGooglePayEnabled == true) {
                setGooglePayRequestDefaults(configuration, braintreeClient.authorization, request)

                val paymentDataRequest =
                    PaymentDataRequest.fromJson(request.toJson())

                val params =
                    GooglePayPaymentAuthRequestParams(
                        getGooglePayEnvironment(configuration),
                        paymentDataRequest
                    )
                callbackPaymentRequestSuccess(
                    GooglePayPaymentAuthRequest.ReadyToLaunch(params),
                    callback
                )
            } else {
                if (configError == null) {
                    callbackPaymentRequestFailure(
                        GooglePayPaymentAuthRequest.Failure(
                            BraintreeException(
                                "Google Pay is not enabled for your Braintree account, " +
                                        "or Google Play Services are not configured correctly."
                            )
                        ), callback
                    )
                } else {
                    callbackPaymentRequestFailure(
                        GooglePayPaymentAuthRequest.Failure(configError),
                        callback
                    )
                }
            }
        }
    }

    /**
     * Call this method when you've received a successful [PaymentData] response from a
     * direct Google Play Services integration to get a [GooglePayCardNonce] or
     * [PayPalAccountNonce].
     *
     * @param paymentData [PaymentData] retrieved from directly integrating with Google Play
     * Services through [PaymentsClient.loadPaymentData]
     * @param callback    [GooglePayTokenizeCallback]
     */
    @SuppressWarnings("TooGenericExceptionCaught")
    fun tokenize(paymentData: PaymentData, callback: GooglePayTokenizeCallback) {
        try {
            val result = JSONObject(paymentData.toJson())
            callbackTokenizeSuccess(GooglePayResult.Success(fromJSON(result)), callback)
        } catch (e: JSONException) {
            try {
                val token =
                    JSONObject(paymentData.toJson()).getJSONObject("paymentMethodData")
                        .getJSONObject("tokenizationData").getString("token")
                callbackTokenizeFailure(GooglePayResult.Failure(fromJson(token)), callback)
            } catch (e1: JSONException) {
                callbackTokenizeFailure(GooglePayResult.Failure(e1), callback)
            } catch (e1: NullPointerException) {
                callbackTokenizeFailure(GooglePayResult.Failure(e1), callback)
            }
        } catch (e: NullPointerException) {
            try {
                val token =
                    JSONObject(paymentData.toJson()).getJSONObject("paymentMethodData")
                        .getJSONObject("tokenizationData").getString("token")
                callbackTokenizeFailure(GooglePayResult.Failure(fromJson(token)), callback)
            } catch (e1: JSONException) {
                callbackTokenizeFailure(GooglePayResult.Failure(e1), callback)
            } catch (e1: NullPointerException) {
                callbackTokenizeFailure(GooglePayResult.Failure(e1), callback)
            }
        }
    }

    /**
     * After a user successfully authorizes Google Pay payment via
     * [GooglePayClient.createPaymentAuthRequest], this
     * method should be invoked to tokenize the payment method to retrieve a
     * [PaymentMethodNonce]
     *
     * @param paymentAuthResult the result of [GooglePayLauncher.launch]
     * @param callback        [GooglePayTokenizeCallback]
     */
    fun tokenize(
        paymentAuthResult: GooglePayPaymentAuthResult,
        callback: GooglePayTokenizeCallback
    ) {
        braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.TOKENIZE_STARTED)
        val paymentData = paymentAuthResult.paymentData
        if (paymentData != null) {
            tokenize(paymentData, callback)
        } else if (paymentAuthResult.error != null) {
            if (paymentAuthResult.error is UserCanceledException) {
                callbackTokenizeCancel(callback)
                return
            }
            callbackTokenizeFailure(GooglePayResult.Failure(paymentAuthResult.error), callback)
        }
    }

    private fun getGooglePayEnvironment(configuration: Configuration): Int {
        return if ("production" == configuration.googlePayEnvironment) {
            WalletConstants.ENVIRONMENT_PRODUCTION
        } else {
            WalletConstants.ENVIRONMENT_TEST
        }
    }

    fun getTokenizationParameters(
        configuration: Configuration,
        authorization: Authorization
    ): PaymentMethodTokenizationParameters {

        val metadata =
            MetadataBuilder().integration(braintreeClient.integrationType)
                .sessionId(analyticsParamRepository.sessionId).version().build()

        val version = try {
            metadata.getString("version")
        } catch (e: JSONException) {
            com.braintreepayments.api.core.BuildConfig.VERSION_NAME
        }

        val parameters =
            PaymentMethodTokenizationParameters.newBuilder().setPaymentMethodTokenizationType(
                WalletConstants.PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY
            )
                .addParameter("gateway", "braintree")
                .addParameter("braintree:merchantId", configuration.merchantId)
                .addParameter("braintree:apiVersion", "v1")
                .addParameter("braintree:sdkVersion", version)
                .addParameter("braintree:metadata", metadata.toString())

        val fingerprint = configuration.googlePayAuthorizationFingerprint
        if (fingerprint?.isNotEmpty() == true) {
            parameters.addParameter("braintree:authorizationFingerprint", fingerprint)
        }

        if (authorization is TokenizationKey) {
            parameters.addParameter("braintree:clientKey", authorization.bearer)
        }

        return parameters.build()
    }

    fun getAllowedCardNetworks(configuration: Configuration): ArrayList<Int> {
        val allowedNetworks = ArrayList<Int>()
        for (network in configuration.googlePaySupportedNetworks) {
            when (network) {
                VISA_NETWORK -> allowedNetworks.add(WalletConstants.CARD_NETWORK_VISA)
                MASTERCARD_NETWORK -> allowedNetworks.add(WalletConstants.CARD_NETWORK_MASTERCARD)
                AMEX_NETWORK -> allowedNetworks.add(WalletConstants.CARD_NETWORK_AMEX)
                DISCOVER_NETWORK -> allowedNetworks.add(WalletConstants.CARD_NETWORK_DISCOVER)
                ELO_NETWORK -> allowedNetworks.add(BraintreeGooglePayWalletConstants.CARD_NETWORK_ELO)
                else -> {}
            }
        }

        return allowedNetworks
    }

    private fun buildCardNetworks(configuration: Configuration): JSONArray {
        val cardNetworkStrings = JSONArray()

        for (network in getAllowedCardNetworks(configuration)) {
            when (network) {
                WalletConstants.CARD_NETWORK_AMEX -> cardNetworkStrings.put("AMEX")
                WalletConstants.CARD_NETWORK_DISCOVER -> cardNetworkStrings.put("DISCOVER")
                WalletConstants.CARD_NETWORK_JCB -> cardNetworkStrings.put("JCB")
                WalletConstants.CARD_NETWORK_MASTERCARD -> cardNetworkStrings.put("MASTERCARD")
                WalletConstants.CARD_NETWORK_VISA -> cardNetworkStrings.put("VISA")
                BraintreeGooglePayWalletConstants.CARD_NETWORK_ELO -> {
                    cardNetworkStrings.put("ELO")
                    cardNetworkStrings.put("ELO_DEBIT")
                }
            }
        }
        return cardNetworkStrings
    }

    private fun buildCardPaymentMethodParameters(
        configuration: Configuration,
        request: GooglePayRequest
    ): JSONObject {
        val defaultParameters = JSONObject()

        try {
            if (request.getAllowedCardNetworksForType(CARD_PAYMENT_TYPE) == null) {
                val cardNetworkStrings = buildCardNetworks(configuration)

                request.getAllowedAuthMethodsForType(CARD_PAYMENT_TYPE)?.let { jsonArray ->
                    request.setAllowedAuthMethods(
                        CARD_PAYMENT_TYPE,
                        jsonArray
                    )
                } ?: run {
                    request.setAllowedAuthMethods(
                        CARD_PAYMENT_TYPE,
                        JSONArray().put("PAN_ONLY").put("CRYPTOGRAM_3DS")
                    )
                }

                request.setAllowedCardNetworks(CARD_PAYMENT_TYPE, cardNetworkStrings)
            }

            defaultParameters.put("billingAddressRequired", request.isBillingAddressRequired)
                .put("allowPrepaidCards", request.allowPrepaidCards)
                .put(
                    "allowedAuthMethods",
                    request.getAllowedAuthMethodsForType(CARD_PAYMENT_TYPE)
                )
                .put(
                    "allowedCardNetworks",
                    request.getAllowedCardNetworksForType(CARD_PAYMENT_TYPE)
                )

            if (request.isBillingAddressRequired) {
                defaultParameters.put(
                    "billingAddressParameters",
                    JSONObject().put("format", request.billingAddressFormatToString())
                        .put("phoneNumberRequired", request.isPhoneNumberRequired)
                )
            }
        } catch (ignored: JSONException) {
        }
        return defaultParameters
    }

    private fun buildPayPalPaymentMethodParameters(configuration: Configuration): JSONObject {
        val defaultParameters = JSONObject()

        try {
            val purchaseContext = JSONObject().put(
                "purchase_units", JSONArray().put(
                    JSONObject().put(
                        "payee", JSONObject().put(
                            "client_id",
                            configuration.googlePayPayPalClientId
                        )
                    )
                        .put("recurring_payment", "true")
                )
            )

            defaultParameters.put("purchase_context", purchaseContext)
        } catch (ignored: JSONException) {
        }

        return defaultParameters
    }

    private fun buildCardTokenizationSpecification(
        configuration: Configuration,
        authorization: Authorization
    ): JSONObject {
        val cardJson = JSONObject()
        val parameters = JSONObject()
        val googlePayVersion = BuildConfig.VERSION_NAME

        try {
            parameters.put("gateway", "braintree").put("braintree:apiVersion", "v1")
                .put("braintree:sdkVersion", googlePayVersion)
                .put("braintree:merchantId", configuration.merchantId)
                .put(
                    "braintree:metadata", JSONObject().put("source", "client")
                        .put("integration", braintreeClient.integrationType)
                        .put("sessionId", analyticsParamRepository.sessionId)
                        .put("version", googlePayVersion)
                        .put("platform", "android").toString()
                )

            if (authorization is TokenizationKey) {
                parameters.put("braintree:clientKey", authorization.toString())
            } else {
                val googlePayAuthFingerprint =
                    configuration.googlePayAuthorizationFingerprint
                parameters.put("braintree:authorizationFingerprint", googlePayAuthFingerprint)
            }
        } catch (ignored: JSONException) {
        }

        try {
            cardJson.put("type", "PAYMENT_GATEWAY").put("parameters", parameters)
        } catch (ignored: JSONException) {
        }

        return cardJson
    }

    private fun buildPayPalTokenizationSpecification(configuration: Configuration): JSONObject {
        val json = JSONObject()
        val googlePayVersion = BuildConfig.VERSION_NAME

        try {
            json.put("type", "PAYMENT_GATEWAY").put(
                "parameters",
                JSONObject().put("gateway", "braintree").put("braintree:apiVersion", "v1")
                    .put("braintree:sdkVersion", googlePayVersion)
                    .put("braintree:merchantId", configuration.merchantId)
                    .put(
                        "braintree:paypalClientId",
                        configuration.googlePayPayPalClientId
                    )
                    .put(
                        "braintree:metadata", JSONObject().put("source", "client")
                            .put("integration", braintreeClient.integrationType)
                            .put("sessionId", analyticsParamRepository.sessionId)
                            .put("version", googlePayVersion)
                            .put("platform", "android").toString()
                    )
            )
        } catch (ignored: JSONException) {
        }

        return json
    }

    private fun setGooglePayRequestDefaults(
        configuration: Configuration,
        authorization: Authorization,
        request: GooglePayRequest
    ) {
        if (request.getAllowedPaymentMethod(CARD_PAYMENT_TYPE) == null) {
            request.setAllowedPaymentMethod(
                CARD_PAYMENT_TYPE,
                buildCardPaymentMethodParameters(configuration, request)
            )
        }

        if (request.getTokenizationSpecificationForType(CARD_PAYMENT_TYPE) == null) {
            request.setTokenizationSpecificationForType(
                "CARD",
                buildCardTokenizationSpecification(configuration, authorization)
            )
        }

        val googlePayCanProcessPayPal = request.isPayPalEnabled &&
                !TextUtils.isEmpty(configuration.googlePayPayPalClientId)

        if (googlePayCanProcessPayPal) {
            if (request.getAllowedPaymentMethod("PAYPAL") == null) {
                request.setAllowedPaymentMethod(
                    PAYPAL_PAYMENT_TYPE,
                    buildPayPalPaymentMethodParameters(configuration)
                )
            }

            if (request.getTokenizationSpecificationForType(PAYPAL_PAYMENT_TYPE) == null) {
                request.setTokenizationSpecificationForType(
                    "PAYPAL",
                    buildPayPalTokenizationSpecification(configuration)
                )
            }
        }

        request.setEnvironment(configuration.googlePayEnvironment)
    }

    private fun validateManifest(): Boolean {
        val activityInfo =
            braintreeClient.getManifestActivityInfo(GooglePayActivity::class.java)
        return activityInfo != null &&
                activityInfo.themeResource == R.style.bt_transparent_activity
    }

    private fun callbackPaymentRequestSuccess(
        request: GooglePayPaymentAuthRequest.ReadyToLaunch,
        callback: GooglePayPaymentAuthRequestCallback
    ) {
        callback.onGooglePayPaymentAuthRequest(request)
        braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.PAYMENT_REQUEST_SUCCEEDED)
    }

    private fun callbackPaymentRequestFailure(
        request: GooglePayPaymentAuthRequest.Failure,
        callback: GooglePayPaymentAuthRequestCallback
    ) {
        callback.onGooglePayPaymentAuthRequest(request)
        braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.PAYMENT_REQUEST_FAILED)
    }

    private fun callbackTokenizeSuccess(
        result: GooglePayResult.Success,
        callback: GooglePayTokenizeCallback
    ) {
        callback.onGooglePayResult(result)
        braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.TOKENIZE_SUCCEEDED)
    }

    private fun callbackTokenizeCancel(callback: GooglePayTokenizeCallback) {
        callback.onGooglePayResult(GooglePayResult.Cancel)
        braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.PAYMENT_SHEET_CANCELED)
    }

    private fun callbackTokenizeFailure(
        result: GooglePayResult.Failure,
        callback: GooglePayTokenizeCallback
    ) {
        callback.onGooglePayResult(result)
        braintreeClient.sendAnalyticsEvent(GooglePayAnalytics.TOKENIZE_FAILED)
    }

    companion object {
        const val EXTRA_ENVIRONMENT: String = "com.braintreepayments.api.EXTRA_ENVIRONMENT"
        const val EXTRA_PAYMENT_DATA_REQUEST: String =
            "com.braintreepayments.api.EXTRA_PAYMENT_DATA_REQUEST"

        private const val VISA_NETWORK = "visa"
        private const val MASTERCARD_NETWORK = "mastercard"
        private const val AMEX_NETWORK = "amex"
        private const val DISCOVER_NETWORK = "discover"
        private const val ELO_NETWORK = "elo"

        private const val CARD_PAYMENT_TYPE = "CARD"
        private const val PAYPAL_PAYMENT_TYPE = "PAYPAL"
    }
}
