package com.braintreepayments.api.paypalsavedpaymentmethod

import android.content.Context
import android.net.Uri
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.core.GraphQLConstants
import com.braintreepayments.api.paypal.PayPalCheckoutRequest
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalPaymentAuthCallback
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Entry point for the saved/editable PayPal payment method feature — implements the fetch/refetch-FI
 * GraphQL calls directly against [BraintreeClient], and starts the edit-FI PayPal payment auth flow
 * via [PayPalClient].
 */
class PayPalSavedPaymentMethodClient internal constructor(
    private val braintreeClient: BraintreeClient,
    private val payPalClient: PayPalClient,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {

    /**
     * Initializes a new [PayPalSavedPaymentMethodClient] instance
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
        braintreeClient = BraintreeClient(
            context = context,
            authorization = authorization,
            deepLinkFallbackUrlScheme = deepLinkFallbackUrlScheme,
            appLinkReturnUri = appLinkReturnUrl
        ),
        payPalClient = PayPalClient(
            context = context,
            authorization = authorization,
            appLinkReturnUrl = appLinkReturnUrl,
            deepLinkFallbackUrlScheme = deepLinkFallbackUrlScheme
        )
    )

    /**
     * Starts the PayPal payment auth flow for the edit-FI checkout using the provided
     * [payPalRequest].
     *
     * @param context       Android Context
     * @param payPalRequest a [PayPalCheckoutRequest] used to customize the request.
     * @param callback      [PayPalPaymentAuthCallback]
     */
    @ExperimentalBetaApi
    fun createPaymentAuthRequest(
        context: Context,
        payPalRequest: PayPalCheckoutRequest,
        callback: PayPalPaymentAuthCallback
    ) = payPalClient.createPaymentAuthRequest(context, payPalRequest, callback)

    /**
     * Fetches the sticky (default) vaulted funding instrument for display.
     *
     * Callback-based variant: the result is delivered asynchronously to [callback]. Use the
     * `suspend` [fetchFI] overload when calling from a coroutine.
     *
     * If [paymentMethodIdJwt] is blank the [callback] receives a
     * [PayPalSavedPaymentMethodSummaryResult.Failure] with a [PayPalSavedPaymentMethodSummaryException].
     *
     * @param paymentMethodIdJwt the `paymentMethodIdJwt` identifying the vaulted funding instrument
     * @param merchantAccountId the merchant account to fetch the funding instrument for; when
     * null, Atmosphere defaults to the merchant's default account.
     * @param callback [PayPalSavedPaymentMethodSummaryCallback] invoked with the result
     */
    @ExperimentalBetaApi
    fun fetchFI(
        paymentMethodIdJwt: String,
        merchantAccountId: String? = null,
        callback: PayPalSavedPaymentMethodSummaryCallback
    ) {
        coroutineScope.launch {
            callback.onPayPalSavedPaymentMethodSummaryResult(fetchFI(paymentMethodIdJwt, merchantAccountId))
        }
    }

    /**
     * Fetches the sticky (default) vaulted funding instrument for display.
     *
     * `suspend` variant: call from a coroutine to receive the result directly as the return value.
     * Use the [fetchFI] overload that takes a [PayPalSavedPaymentMethodSummaryCallback] outside a
     * coroutine.
     *
     * If [paymentMethodIdJwt] is blank a [PayPalSavedPaymentMethodSummaryResult.Failure] with a
     * [PayPalSavedPaymentMethodSummaryException] is returned.
     *
     * @param paymentMethodIdJwt the `paymentMethodIdJwt` identifying the vaulted funding instrument
     * @param merchantAccountId the merchant account to fetch the funding instrument for; when
     * null, Atmosphere defaults to the merchant's default account.
     * @return [PayPalSavedPaymentMethodSummaryResult]
     */
    @ExperimentalBetaApi
    suspend fun fetchFI(
        paymentMethodIdJwt: String,
        merchantAccountId: String? = null
    ): PayPalSavedPaymentMethodSummaryResult {
        if (paymentMethodIdJwt.isBlank()) {
            return PayPalSavedPaymentMethodSummaryResult.Failure(
                PayPalSavedPaymentMethodSummaryException(
                    errorClass = null,
                    message = PayPalSavedPaymentMethodSummaryException.MISSING_PAYMENT_METHOD_ID_JWT,
                )
            )
        }
        return getPaymentMethod(
            GetPayPalSavedPaymentMethodGraphQLBody.stickyFi(paymentMethodIdJwt, merchantAccountId)
        )
    }

    /**
     * Refreshes the vaulted funding instrument after an edit, keyed by the approved-checkout order
     * id.
     *
     * Callback-based variant: the result is delivered asynchronously to [callback]. Use the
     * `suspend` [refetchFI] overload when calling from a coroutine.
     *
     * @param orderId  the approved-checkout order id
     * @param merchantAccountId the merchant account to fetch the funding instrument for; when
     * null, Atmosphere defaults to the merchant's default account.
     * @param callback [PayPalSavedPaymentMethodSummaryCallback] invoked with the result
     */
    @ExperimentalBetaApi
    fun refetchFI(
        orderId: String,
        merchantAccountId: String? = null,
        callback: PayPalSavedPaymentMethodSummaryCallback
    ) {
        coroutineScope.launch {
            callback.onPayPalSavedPaymentMethodSummaryResult(refetchFI(orderId, merchantAccountId))
        }
    }

    /**
     * Refreshes the vaulted funding instrument after an edit, keyed by the approved-checkout order
     * id.
     *
     * `suspend` variant: call from a coroutine to receive the result directly as the return value.
     * Use the [refetchFI] overload that takes a [PayPalSavedPaymentMethodSummaryCallback] outside a
     * coroutine.
     *
     * @param orderId the approved-checkout order id
     * @param merchantAccountId the merchant account to fetch the funding instrument for; when
     * null, Atmosphere defaults to the merchant's default account.
     * @return [PayPalSavedPaymentMethodSummaryResult]
     */
    @ExperimentalBetaApi
    suspend fun refetchFI(
        orderId: String,
        merchantAccountId: String? = null
    ): PayPalSavedPaymentMethodSummaryResult =
        getPaymentMethod(
            GetPayPalSavedPaymentMethodGraphQLBody.fromApprovedCheckout(orderId, merchantAccountId)
        )

    @OptIn(ExperimentalBetaApi::class)
    @Suppress("TooGenericExceptionCaught")
    private suspend fun getPaymentMethod(body: JSONObject): PayPalSavedPaymentMethodSummaryResult =
        try {
            val response = JSONObject(braintreeClient.sendGraphQLPOST(body))
            val errors = response.optJSONArray(GraphQLConstants.Keys.ERRORS)
            if (errors != null && errors.length() > 0) {
                throw PayPalSavedPaymentMethodSummaryException.fromGraphQLResponse(response)
            }
            PayPalSavedPaymentMethodSummaryResult.Success(PayPalSavedPaymentMethodSummary.fromJson(response))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            PayPalSavedPaymentMethodSummaryResult.Failure(e)
        }

    /**
     * Fetches PayPal Pay Later / Credit presentment messaging for the edit-FI row.
     *
     * Callback-based variant: the result is delivered asynchronously to [callback]. Use the
     * `suspend` [fetchCreditPresentmentMessages] overload when calling from a coroutine.
     *
     * @param request  [PayPalCreditMessagingRequest]
     * @param callback [PayPalCreditMessagingCallback] invoked with the result
     */
    @ExperimentalBetaApi
    fun fetchCreditPresentmentMessages(
        request: PayPalCreditMessagingRequest,
        callback: PayPalCreditMessagingCallback
    ) {
        coroutineScope.launch {
            callback.onPayPalCreditMessagingResult(fetchCreditPresentmentMessages(request))
        }
    }

    /**
     * Fetches PayPal Pay Later / Credit presentment messaging for the edit-FI row.
     *
     * `suspend` variant: call from a coroutine to receive the result directly as the return value.
     * Use the [fetchCreditPresentmentMessages] overload that takes a
     * [PayPalCreditMessagingCallback] outside a coroutine.
     *
     * @param request [PayPalCreditMessagingRequest]
     * @return [PayPalCreditMessagingResult], or null if the fetch fails or returns no
     * `preferred_message` - callers should hide the messaging row; the FI card still renders.
     */
    @ExperimentalBetaApi
    @Suppress("TooGenericExceptionCaught")
    suspend fun fetchCreditPresentmentMessages(
        request: PayPalCreditMessagingRequest
    ): PayPalCreditMessagingResult? = try {
        val configuration = braintreeClient.getConfiguration()
        val baseUrl = when (configuration.environment) {
            "production" -> PRODUCTION_BASE_URL
            else -> SANDBOX_BASE_URL
        }
        val responseBody = braintreeClient.sendPOST(
            url = "$baseUrl$CREDIT_PRESENTMENT_MESSAGES_PATH",
            data = request.build().toString()
        )
        JSONObject(responseBody)
            .optJSONArray(MESSAGES_KEY)
            ?.optJSONObject(0)
            ?.takeIf { it.has(PREFERRED_MESSAGE_KEY) }
            ?.let { PayPalCreditMessagingResult.fromJson(it) }
    } catch (e: Exception) {
        if (e is CancellationException) throw e
        null
    }

    companion object {
        private const val PRODUCTION_BASE_URL = "https://api.paypal.com"
        private const val SANDBOX_BASE_URL = "https://api.sandbox.paypal.com"
        private const val CREDIT_PRESENTMENT_MESSAGES_PATH = "/v2/credit/fetch-presentment-messages"
        private const val MESSAGES_KEY = "messages"
        private const val PREFERRED_MESSAGE_KEY = "preferred_message"
    }
}
