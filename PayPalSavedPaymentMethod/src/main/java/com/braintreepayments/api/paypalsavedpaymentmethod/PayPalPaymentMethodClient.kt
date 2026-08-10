package com.braintreepayments.api.paypalsavedpaymentmethod

import android.content.Context
import android.net.Uri
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.core.GraphQLConstants
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalPaymentAuthCallback
import com.braintreepayments.api.paypal.PayPalRequest
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
class PayPalPaymentMethodClient internal constructor(
    private val braintreeClient: BraintreeClient,
    private val payPalClient: PayPalClient,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {

    /**
     * Initializes a new [PayPalPaymentMethodClient] instance
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
     * @param payPalRequest a [PayPalRequest] used to customize the request.
     * @param callback      [PayPalPaymentAuthCallback]
     */
    @ExperimentalBetaApi
    fun createPaymentAuthRequest(
        context: Context,
        payPalRequest: PayPalRequest,
        callback: PayPalPaymentAuthCallback
    ) = payPalClient.createPaymentAuthRequest(context, payPalRequest, callback)

    /**
     * Fetches the sticky (default) vaulted funding instrument for display.
     *
     * Callback-based variant: the result is delivered asynchronously to [callback]. Use the
     * `suspend` [fetchFI] overload when calling from a coroutine.
     *
     * If [paymentMethodIdJwt] is blank the [callback] receives a
     * [PayPalPaymentMethodSummaryResult.Failure] with a [PayPalPaymentMethodSummaryException].
     *
     * @param paymentMethodIdJwt the `paymentMethodIdJwt` identifying the vaulted funding instrument
     * @param callback [PayPalPaymentMethodSummaryCallback] invoked with the result
     */
    @ExperimentalBetaApi
    fun fetchFI(paymentMethodIdJwt: String, callback: PayPalPaymentMethodSummaryCallback) {
        coroutineScope.launch {
            callback.onPayPalPaymentMethodSummaryResult(fetchFI(paymentMethodIdJwt))
        }
    }

    /**
     * Fetches the sticky (default) vaulted funding instrument for display.
     *
     * `suspend` variant: call from a coroutine to receive the result directly as the return value.
     * Use the [fetchFI] overload that takes a [PayPalPaymentMethodSummaryCallback] outside a
     * coroutine.
     *
     * If [paymentMethodIdJwt] is blank a [PayPalPaymentMethodSummaryResult.Failure] with a
     * [PayPalPaymentMethodSummaryException] is returned.
     *
     * @param paymentMethodIdJwt the `paymentMethodIdJwt` identifying the vaulted funding instrument
     * @return [PayPalPaymentMethodSummaryResult]
     */
    @ExperimentalBetaApi
    suspend fun fetchFI(paymentMethodIdJwt: String): PayPalPaymentMethodSummaryResult {
        if (paymentMethodIdJwt.isBlank()) {
            return PayPalPaymentMethodSummaryResult.Failure(
                PayPalPaymentMethodSummaryException(
                    errorClass = null,
                    message = PayPalPaymentMethodSummaryException.MISSING_PAYMENT_METHOD_ID_JWT,
                )
            )
        }
        return getSavedPaypalPaymentMethod(
            GetSavedPaypalPaymentMethodGraphQLBody.stickyFi(paymentMethodIdJwt)
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
     * @param callback [PayPalPaymentMethodSummaryCallback] invoked with the result
     */
    @ExperimentalBetaApi
    fun refetchFI(orderId: String, callback: PayPalPaymentMethodSummaryCallback) {
        coroutineScope.launch {
            callback.onPayPalPaymentMethodSummaryResult(refetchFI(orderId))
        }
    }

    /**
     * Refreshes the vaulted funding instrument after an edit, keyed by the approved-checkout order
     * id.
     *
     * `suspend` variant: call from a coroutine to receive the result directly as the return value.
     * Use the [refetchFI] overload that takes a [PayPalPaymentMethodSummaryCallback] outside a
     * coroutine.
     *
     * @param orderId the approved-checkout order id
     * @return [PayPalPaymentMethodSummaryResult]
     */
    @ExperimentalBetaApi
    suspend fun refetchFI(orderId: String): PayPalPaymentMethodSummaryResult =
        getSavedPaypalPaymentMethod(
            GetSavedPaypalPaymentMethodGraphQLBody.fromApprovedCheckout(orderId)
        )

    @OptIn(ExperimentalBetaApi::class)
    @Suppress("TooGenericExceptionCaught")
    private suspend fun getSavedPaypalPaymentMethod(body: JSONObject): PayPalPaymentMethodSummaryResult =
        try {
            val response = JSONObject(braintreeClient.sendGraphQLPOST(body))
            val errors = response.optJSONArray(GraphQLConstants.Keys.ERRORS)
            if (errors != null && errors.length() > 0) {
                throw PayPalPaymentMethodSummaryException.fromGraphQLResponse(response)
            }
            PayPalPaymentMethodSummaryResult.Success(PayPalPaymentMethodSummary.fromJson(response))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            PayPalPaymentMethodSummaryResult.Failure(e)
        }
}
