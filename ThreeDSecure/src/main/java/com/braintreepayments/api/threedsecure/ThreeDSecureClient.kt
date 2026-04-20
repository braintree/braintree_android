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
import kotlin.coroutines.cancellation.CancellationException

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

        coroutineScope.launch {
            val result = createPaymentAuthRequest(context, request)
            callback.onThreeDSecurePaymentAuthRequest(result)
        }
    }

    private suspend fun createPaymentAuthRequest(
        context: Context,
        request: ThreeDSecureRequest
    ): ThreeDSecurePaymentAuthRequest {
        if (request.amount == null || request.nonce == null) {
            return createPaymentAuthFailure(
                InvalidArgumentException(
                    "The ThreeDSecureRequest nonce and amount cannot be null"
                )
            )
        }
        try {
            val configuration = braintreeClient.getConfiguration()
            return when {
                !configuration.isThreeDSecureEnabled -> {
                    createPaymentAuthFailure(
                        BraintreeException(
                            "Three D Secure is not enabled for this account. " +
                                    "Please contact Braintree Support for assistance."
                        )
                    )
                }
                configuration.cardinalAuthenticationJwt == null -> {
                    createPaymentAuthFailure(
                        BraintreeException(
                            "Merchant is not configured for 3DS 2.0. " +
                                    "Please contact Braintree Support for assistance."
                        )
                    )
                }
                else -> {
                    initializeCardinalClient(context, configuration, request)
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            return createPaymentAuthFailure(e)
        }
    }

    private suspend fun initializeCardinalClient(
        context: Context,
        configuration: Configuration,
        request: ThreeDSecureRequest
    ): ThreeDSecurePaymentAuthRequest {
        try {
            cardinalClient.initialize(
                context = context,
                configuration = configuration,
                request = request
            )
            try {
                val threeDSecureResult = api.performLookup(
                    request = request,
                    cardinalConsumerSessionId = cardinalClient.consumerSessionId
                )
                return sendAnalyticsAndResult(threeDSecureResult)
            } catch (performLookupError: Exception) {
                if (performLookupError is CancellationException) throw performLookupError
                braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.LOOKUP_FAILED)
                return createPaymentAuthFailure(
                    BraintreeException("3DS lookup failed", performLookupError)
                )
            }
        } catch (initializeException: BraintreeException) {
            return createPaymentAuthFailure(initializeException)
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
    fun prepareLookup(
        context: Context,
        request: ThreeDSecureRequest,
        callback: ThreeDSecurePrepareLookupCallback
    ) {
        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_STARTED)

        coroutineScope.launch {
            val result = prepareLookup(context, request)
            callback.onPrepareLookupResult(result)
        }
    }

    private suspend fun prepareLookup(
        context: Context,
        request: ThreeDSecureRequest
    ): ThreeDSecurePrepareLookupResult {
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

        try {
            val configuration = braintreeClient.getConfiguration()
            if (configuration.cardinalAuthenticationJwt == null) {
                return prepareLookupFailure(
                    BraintreeException(
                        "Merchant is not configured for 3DS 2.0. " +
                                "Please contact Braintree Support for assistance."
                    )
                )
            }

            cardinalClient.initialize(
                context,
                configuration,
                request
            )
            val consumerSessionId = cardinalClient.consumerSessionId
            if (!consumerSessionId.isNullOrEmpty()) {
                lookupJSON.put("dfReferenceId", consumerSessionId)
            } else {
                return prepareLookupFailure(
                        BraintreeException("There was an error retrieving the dfReferenceId.")
                    )
            }
            braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.LOOKUP_SUCCEEDED)
            return ThreeDSecurePrepareLookupResult.Success(
                    request,
                    lookupJSON.toString()
                )
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            return prepareLookupFailure(error)
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
        coroutineScope.launch {
            val result = try {
                braintreeClient.getConfiguration()
                sendAnalyticsAndResult(fromJson(lookupResponse))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.LOOKUP_FAILED)
                createPaymentAuthFailure(e)
            }
            callback.onThreeDSecurePaymentAuthRequest(result)
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    fun sendAnalyticsAndResult(
        result: ThreeDSecureParams
    ): ThreeDSecurePaymentAuthRequest {
        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.LOOKUP_SUCCEEDED)
        val lookup = result.lookup
        val threeDSecureNonce = result.threeDSecureNonce
        val showChallenge = lookup?.acsUrl != null
        val threeDSecureVersion = lookup?.threeDSecureVersion

        if (!showChallenge) {
            if (threeDSecureNonce != null && lookup != null) {
                 return ThreeDSecurePaymentAuthRequest.LaunchNotRequired(
                    threeDSecureNonce,
                    lookup
                )
            } else {
                return createPaymentAuthFailure(
                    BraintreeException("lookup and threeDSecureNonce are null")
                )
            }
        }

        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.CHALLENGE_REQUIRED)

        if (threeDSecureVersion?.startsWith("2.") == false) {
            val threeDSecureV1UnsupportedMessage =
                "3D Secure v1 is deprecated and no longer supported. See " +
                    "https://developer.paypal.com/braintree/docs/guides/3d-secure/client-side/android/v4 " +
                    "for more information."
             return createPaymentAuthFailure(
                     BraintreeException(threeDSecureV1UnsupportedMessage)
            )
        }

        return ThreeDSecurePaymentAuthRequest.ReadyToLaunch(result)
    }

    /**
     * Call this method from the [ThreeDSecureLauncherCallback] passed to the
     * [ThreeDSecureLauncher] used to launch the 3DS authentication challenge.
     *
     * @param paymentAuthResult a [ThreeDSecurePaymentAuthResult] received in [ThreeDSecureLauncherCallback]
     * @param callback       a [ThreeDSecureResultCallback]
     */
    fun tokenize(
        paymentAuthResult: ThreeDSecurePaymentAuthResult,
        callback: ThreeDSecureTokenizeCallback
    ) {
        coroutineScope.launch {
            val result = tokenize(paymentAuthResult)
            callback.onThreeDSecureResult(result)
        }
    }

    @Suppress("LongMethod")
    private suspend fun tokenize(
        paymentAuthResult: ThreeDSecurePaymentAuthResult
    ): ThreeDSecureResult {
        val threeDSecureError = paymentAuthResult.error
        if (threeDSecureError != null) {
            return tokenizeFailure(threeDSecureError, null)
        }

        val threeDSecureParams = paymentAuthResult.threeDSecureParams
        val validateResponse = paymentAuthResult.validateResponse
        val jwt = paymentAuthResult.jwt

        return when (validateResponse?.actionCode) {
            CardinalActionCode.FAILURE,
            CardinalActionCode.NOACTION,
            CardinalActionCode.SUCCESS -> {
                try {
                    val threeDSecureResult = api.authenticateCardinalJWT(
                        threeDSecureParams = threeDSecureParams,
                        cardinalJWT = jwt
                    )
                    if (threeDSecureResult.hasError()) {
                        braintreeClient.sendAnalyticsEvent(
                            ThreeDSecureAnalytics.JWT_AUTH_FAILED,
                            AnalyticsEventParams(errorDescription = threeDSecureResult.errorMessage)
                        )
                        tokenizeFailure(
                            BraintreeException(threeDSecureResult.errorMessage),
                            threeDSecureResult.threeDSecureNonce
                        )
                    } else {
                        threeDSecureResult.threeDSecureNonce?.let {
                            braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.JWT_AUTH_SUCCEEDED)
                            tokenizeSuccess(it)
                        } ?: tokenizeFailure(
                            BraintreeException("ThreeDSecure nonce was null"),
                            null
                        )
                    }
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    braintreeClient.sendAnalyticsEvent(
                        ThreeDSecureAnalytics.JWT_AUTH_FAILED,
                        AnalyticsEventParams(errorDescription = error.message)
                    )
                    tokenizeFailure(error, null)
                }
            }

            CardinalActionCode.ERROR, CardinalActionCode.TIMEOUT -> {
                tokenizeFailure(BraintreeException(validateResponse.errorDescription), null)
            }

            CardinalActionCode.CANCEL -> {
                braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_CANCELED)
                ThreeDSecureResult.Cancel
            }

            else -> {
                val errorDescription = "invalid action code"
                braintreeClient.sendAnalyticsEvent(
                    ThreeDSecureAnalytics.JWT_AUTH_FAILED,
                    AnalyticsEventParams(errorDescription = errorDescription)
                )
                tokenizeFailure(BraintreeException(errorDescription), null)
            }
        }
    }

    private fun createPaymentAuthFailure(
       error: Exception
    ): ThreeDSecurePaymentAuthRequest.Failure {
        braintreeClient.sendAnalyticsEvent(
            ThreeDSecureAnalytics.VERIFY_FAILED,
            AnalyticsEventParams(errorDescription = error.message)
        )
        return ThreeDSecurePaymentAuthRequest.Failure(error)
    }

    private fun prepareLookupFailure(
        error: Exception
    ): ThreeDSecurePrepareLookupResult.Failure {
        braintreeClient.sendAnalyticsEvent(
            ThreeDSecureAnalytics.LOOKUP_FAILED,
            AnalyticsEventParams(errorDescription = error.message)
        )
        braintreeClient.sendAnalyticsEvent(
            ThreeDSecureAnalytics.VERIFY_FAILED,
            AnalyticsEventParams(errorDescription = error.message)
        )
        return ThreeDSecurePrepareLookupResult.Failure(error)
    }

    private fun tokenizeFailure(
        error: Exception,
        nonce: ThreeDSecureNonce?
    ): ThreeDSecureResult.Failure {
        braintreeClient.sendAnalyticsEvent(
            ThreeDSecureAnalytics.VERIFY_FAILED,
            AnalyticsEventParams(errorDescription = error.message)
        )
        return ThreeDSecureResult.Failure(error, nonce)
    }

    private fun tokenizeSuccess(
        nonce: ThreeDSecureNonce
    ): ThreeDSecureResult.Success {
        braintreeClient.sendAnalyticsEvent(ThreeDSecureAnalytics.VERIFY_SUCCEEDED)
        return ThreeDSecureResult.Success(nonce)
    }
}
