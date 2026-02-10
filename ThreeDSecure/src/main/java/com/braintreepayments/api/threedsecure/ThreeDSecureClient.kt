package com.braintreepayments.api.threedsecure

import android.content.Context
import androidx.annotation.RestrictTo
import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.BuildConfig
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.InvalidArgumentException
import com.braintreepayments.api.core.MerchantRepository
import com.braintreepayments.api.threedsecure.ThreeDSecureParams.Companion.fromJson
import com.cardinalcommerce.cardinalmobilesdk.models.CardinalActionCode
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/**
 * 3D Secure is a protocol that enables cardholders and issuers to add a layer of security to
 * e-commerce transactions via password entry at checkout.
 *
 * One of the primary reasons to use 3D Secure is to benefit from a shift in liability from the
 * merchant to the issuer, which may result in interchange savings. Please read our online [documentation](https://developer.paypal.com/braintree/docs/guides/3d-secure/overview)
 * for a full explanation of 3D Secure.
 */
@Suppress("TooManyFunctions")
class ThreeDSecureClient internal constructor(
    private val braintreeClient: BraintreeClient,
    private val cardinalClient: CardinalClient = CardinalClient(),
    private val api: ThreeDSecureAPI = ThreeDSecureAPI(braintreeClient),
    private val merchantRepository: MerchantRepository = MerchantRepository.instance,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val coroutineScope: CoroutineScope = CoroutineScope(dispatcher)
) {
    /**
     * Initializes a new [ThreeDSecureClient] instance
     *
     * @param context an Android Context
     * @param authorization a Tokenization Key or Client Token used to authenticate
     */
    constructor(
        context: Context,
        authorization: String
    ) : this(BraintreeClient(context, authorization))

    /**
     * Call this method to initiate the 3D Secure flow.
     *
     * Verification is associated with a transaction amount and your merchant account. To specify a
     * different merchant account (or, in turn, currency), you will need to specify the merchant
     * account id when [
     * generating a client token](https://developer.paypal.com/braintree/docs/start/hello-client#get-a-client-token)
     *
     * During lookup the original payment method nonce is consumed and a new one is returned, which
     * points to the original payment method, as well as the 3D Secure verification. Transactions
     * created with this nonce will be 3D Secure, and benefit from the appropriate liability shift
     * if authentication is successful or fail with a 3D Secure failure.
     *
     * @param context  Android context
     * @param request  the [ThreeDSecureRequest] with information used for authentication.
     * @param callback [ThreeDSecureResultCallback]
     */
    fun createPaymentAuthRequest(
        context: Context,
        request: ThreeDSecureRequest,
        callback: ThreeDSecurePaymentAuthRequestCallback
    ) {
        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_STARTED)
        if (request.amount == null || request.nonce == null) {
            callbackCreatePaymentAuthFailure(
                callback,
                ThreeDSecurePaymentAuthRequest.Failure(
                    InvalidArgumentException(
                        "The ThreeDSecureRequest nonce and amount cannot be null"
                    )
                )
            )
            return
        }
        coroutineScope.launch {
            try {
                val configuration = braintreeClient.getConfiguration()
                when {
                    !configuration.isThreeDSecureEnabled -> {
                        val failure = BraintreeException(
                            "Three D Secure is not enabled for this account. " +
                                    "Please contact Braintree Support for assistance."
                        )
                        callbackCreatePaymentAuthFailure(callback, ThreeDSecurePaymentAuthRequest.Failure(failure))
                    }
                    configuration.cardinalAuthenticationJwt == null -> {
                        val failure = BraintreeException(
                            "Merchant is not configured for 3DS 2.0. " +
                                    "Please contact Braintree Support for assistance."
                        )
                        callbackCreatePaymentAuthFailure(callback, ThreeDSecurePaymentAuthRequest.Failure(failure))
                    }

                    else -> initializeCardinalClient(context, configuration, request, callback)
                }
            } catch (e: IOException) {
                callbackCreatePaymentAuthFailure(
                    callback,
                    ThreeDSecurePaymentAuthRequest.Failure(e)
                )
            }
        }
    }

    private fun initializeCardinalClient(
        context: Context,
        configuration: Configuration,
        request: ThreeDSecureRequest,
        callback: ThreeDSecurePaymentAuthRequestCallback
    ) {
        try {
            cardinalClient.initialize(
                context = context,
                configuration = configuration,
                request = request
            ) { _, _ ->
                api.performLookup(
                    request = request,
                    cardinalConsumerSessionId = cardinalClient.consumerSessionId
                ) { threeDSecureResult: ThreeDSecureParams?, performLookupError: Exception? ->
                    if (threeDSecureResult != null) {
                        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.LOOKUP_SUCCEEDED)
                        sendAnalyticsAndCallbackResult(threeDSecureResult, callback)
                    } else {
                        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.LOOKUP_FAILED)
                        callbackCreatePaymentAuthFailure(
                            callback,
                            ThreeDSecurePaymentAuthRequest.Failure(
                                performLookupError ?: BraintreeException("3DS lookup failed")
                            )
                        )
                    }
                }
            }
        } catch (initializeException: BraintreeException) {
            callbackCreatePaymentAuthFailure(
                callback,
                ThreeDSecurePaymentAuthRequest.Failure(initializeException)
            )
        }
    }

    /**
     * Creates a stringified JSON object containing the information necessary to perform a lookup
     *
     * @param context  Android Context
     * @param request  the [ThreeDSecureRequest] that has a nonce and an optional UI
     * customization.
     * @param callback [ThreeDSecurePrepareLookupCallback]
     */
    @Suppress("LongMethod")
    fun prepareLookup(
        context: Context,
        request: ThreeDSecureRequest,
        callback: ThreeDSecurePrepareLookupCallback
    ) {
        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_STARTED)
        val lookupJSON = JSONObject()
        try {
            lookupJSON
                .put("authorizationFingerprint", merchantRepository.authorization.bearer)
                .put("braintreeLibraryVersion", "Android-${BuildConfig.VERSION_NAME}")
                .put("nonce", request.nonce)
                .put(
                    "clientMetadata",
                    JSONObject()
                        .put("requestedThreeDSecureVersion", "2")
                        .put("sdkVersion", "Android/${BuildConfig.VERSION_NAME}")
                )
        } catch (ignored: JSONException) {
        }

        braintreeClient.getConfiguration { configuration: Configuration?, configError: Exception? ->
            if (configuration == null) {
                callbackPrepareLookupFailure(
                    callback,
                    ThreeDSecurePrepareLookupResult.Failure(
                        configError ?: BraintreeException("Error getting configuration")
                    )
                )
                return@getConfiguration
            }
            if (configuration.cardinalAuthenticationJwt == null) {
                callbackPrepareLookupFailure(
                    callback,
                    ThreeDSecurePrepareLookupResult.Failure(
                        BraintreeException(
                            "Merchant is not configured for 3DS 2.0. " +
                                "Please contact Braintree Support for assistance."
                        )
                    )
                )
                return@getConfiguration
            }

            try {
                cardinalClient.initialize(
                    context,
                    configuration,
                    request
                ) { consumerSessionId: String?, _ ->
                    if (!consumerSessionId.isNullOrEmpty()) {
                            lookupJSON.put("dfReferenceId", consumerSessionId)
                    } else {
                        callbackPrepareLookupFailure(
                            callback,
                            ThreeDSecurePrepareLookupResult.Failure(
                                BraintreeException("There was an error retrieving the dfReferenceId.")
                            )
                        )

                        return@initialize
                    }
                    braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.LOOKUP_SUCCEEDED)
                    callback.onPrepareLookupResult(
                        ThreeDSecurePrepareLookupResult.Success(
                            request,
                            lookupJSON.toString()
                        )
                    )
                }
            } catch (initializeException: BraintreeException) {
                callbackPrepareLookupFailure(
                    callback,
                    ThreeDSecurePrepareLookupResult.Failure(initializeException)
                )
            }
        }
        coroutineScope.launch {
            try {
                val configuration = braintreeClient.getConfiguration()
            }
        }
    }

    /**
     * Initialize a challenge from a server side lookup call.
     *
     * @param lookupResponse The lookup response from the server side call to lookup the 3D Secure
     * information.
     * @param callback       [ThreeDSecureResultCallback]
     */
    fun initializeChallengeWithLookupResponse(
        lookupResponse: String,
        callback: ThreeDSecurePaymentAuthRequestCallback
    ) {
        braintreeClient.getConfiguration { _, _ ->
            try {
                sendAnalyticsAndCallbackResult(fromJson(lookupResponse), callback)
            } catch (e: JSONException) {
                braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.LOOKUP_FAILED)
                callbackCreatePaymentAuthFailure(
                    callback,
                    ThreeDSecurePaymentAuthRequest.Failure(e)
                )
            }
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun sendAnalyticsAndCallbackResult(
        result: ThreeDSecureParams,
        callback: ThreeDSecurePaymentAuthRequestCallback
    ) {
        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.LOOKUP_SUCCEEDED)
        val lookup = result.lookup
        val threeDSecureNonce = result.threeDSecureNonce
        val showChallenge = lookup?.acsUrl != null
        val threeDSecureVersion = lookup?.threeDSecureVersion

        if (!showChallenge) {
            if (threeDSecureNonce != null && lookup != null) {
                callback.onThreeDSecurePaymentAuthRequest(
                    ThreeDSecurePaymentAuthRequest.LaunchNotRequired(
                        threeDSecureNonce,
                        lookup
                    )
                )
            } else {
                callbackCreatePaymentAuthFailure(
                    callback,
                    ThreeDSecurePaymentAuthRequest.Failure(
                        BraintreeException("lookup and threeDSecureNonce are null")
                    )
                )
            }
            return
        }

        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.CHALLENGE_REQUIRED)

        if (threeDSecureVersion?.startsWith("2.") == false) {
            val threeDSecureV1UnsupportedMessage =
                "3D Secure v1 is deprecated and no longer supported. See " +
                    "https://developer.paypal.com/braintree/docs/guides/3d-secure/client-side/android/v4 " +
                    "for more information."
            callbackCreatePaymentAuthFailure(
                callback,
                ThreeDSecurePaymentAuthRequest.Failure(
                    BraintreeException(threeDSecureV1UnsupportedMessage)
                )
            )
            return
        }

        callback.onThreeDSecurePaymentAuthRequest(
            ThreeDSecurePaymentAuthRequest.ReadyToLaunch(result)
        )
    }

    /**
     * Call this method from the [ThreeDSecureLauncherCallback] passed to the
     * [ThreeDSecureLauncher] used to launch the 3DS authentication challenge.
     *
     * @param paymentAuthResult a [ThreeDSecurePaymentAuthResult] received in [ThreeDSecureLauncherCallback]
     * @param callback       a [ThreeDSecureResultCallback]
     */
    @Suppress("LongMethod")
    fun tokenize(
        paymentAuthResult: ThreeDSecurePaymentAuthResult,
        callback: ThreeDSecureTokenizeCallback
    ) {
        val threeDSecureError = paymentAuthResult.error
        if (threeDSecureError != null) {
            callbackTokenizeFailure(callback, ThreeDSecureResult.Failure(threeDSecureError, null))
        } else {
            val threeDSecureParams = paymentAuthResult.threeDSecureParams
            val validateResponse = paymentAuthResult.validateResponse
            val jwt = paymentAuthResult.jwt

            when (validateResponse?.actionCode) {
                CardinalActionCode.FAILURE,
                CardinalActionCode.NOACTION,
                CardinalActionCode.SUCCESS -> api.authenticateCardinalJWT(
                    threeDSecureParams = threeDSecureParams,
                    cardinalJWT = jwt
                ) { threeDSecureResult: ThreeDSecureParams?, error: Exception? ->
                    if (threeDSecureResult != null) {
                        if (threeDSecureResult.hasError()) {
                            braintreeClient.sendAnalyticsEvent(
                                ThreeDSecureAnalytics.JWT_AUTH_FAILED,
                                AnalyticsEventParams(errorDescription = threeDSecureResult.errorMessage)
                            )
                            callbackTokenizeFailure(
                                callback,
                                ThreeDSecureResult.Failure(
                                    BraintreeException(threeDSecureResult.errorMessage),
                                    threeDSecureResult.threeDSecureNonce
                                )
                            )
                        } else {
                            threeDSecureResult.threeDSecureNonce?.let {
                                braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.JWT_AUTH_SUCCEEDED)
                                callbackTokenizeSuccess(callback, ThreeDSecureResult.Success(it))
                            }
                        }
                    } else if (error != null) {
                        braintreeClient.sendAnalyticsEvent(
                            ThreeDSecureAnalytics.JWT_AUTH_FAILED,
                            AnalyticsEventParams(errorDescription = error.message)
                        )
                        callbackTokenizeFailure(
                            callback,
                            ThreeDSecureResult.Failure(
                                error,
                                null
                            )
                        )
                    }
                }

                CardinalActionCode.ERROR, CardinalActionCode.TIMEOUT -> callbackTokenizeFailure(
                    callback, ThreeDSecureResult.Failure(
                        BraintreeException(validateResponse.errorDescription), null
                    )
                )

                CardinalActionCode.CANCEL -> callbackCancel(callback)

                else -> {
                    val errorDescription = "invalid action code"
                    braintreeClient.sendAnalyticsEvent(
                        ThreeDSecureAnalytics.JWT_AUTH_FAILED,
                        AnalyticsEventParams(errorDescription = errorDescription)
                    )
                    callbackTokenizeFailure(
                        callback,
                        ThreeDSecureResult.Failure(
                            error = BraintreeException(errorDescription),
                            nonce = null
                        )
                    )
                }
            }
        }
    }

    private fun callbackCreatePaymentAuthFailure(
        callback: ThreeDSecurePaymentAuthRequestCallback,
        failure: ThreeDSecurePaymentAuthRequest.Failure
    ) {
        braintreeClient.sendAnalyticsEvent(
            ThreeDSecureAnalytics.VERIFY_FAILED,
            AnalyticsEventParams(errorDescription = failure.error.message)
        )
        callback.onThreeDSecurePaymentAuthRequest(failure)
    }

    private fun callbackPrepareLookupFailure(
        callback: ThreeDSecurePrepareLookupCallback,
        result: ThreeDSecurePrepareLookupResult.Failure
    ) {
        braintreeClient.sendAnalyticsEvent(
            ThreeDSecureAnalytics.LOOKUP_FAILED,
            AnalyticsEventParams(errorDescription = result.error.message)
        )
        braintreeClient.sendAnalyticsEvent(
            ThreeDSecureAnalytics.VERIFY_FAILED,
            AnalyticsEventParams(errorDescription = result.error.message)
        )
        callback.onPrepareLookupResult(result)
    }

    private fun callbackTokenizeFailure(
        callback: ThreeDSecureTokenizeCallback,
        result: ThreeDSecureResult.Failure
    ) {
        braintreeClient.sendAnalyticsEvent(
            ThreeDSecureAnalytics.VERIFY_FAILED,
            AnalyticsEventParams(errorDescription = result.error.message)
        )
        callback.onThreeDSecureResult(result)
    }

    private fun callbackTokenizeSuccess(
        callback: ThreeDSecureTokenizeCallback,
        result: ThreeDSecureResult.Success
    ) {
        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_SUCCEEDED)
        callback.onThreeDSecureResult(result)
    }

    private fun callbackCancel(callback: ThreeDSecureTokenizeCallback) {
        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_CANCELED)
        callback.onThreeDSecureResult(ThreeDSecureResult.Cancel)
    }
}
