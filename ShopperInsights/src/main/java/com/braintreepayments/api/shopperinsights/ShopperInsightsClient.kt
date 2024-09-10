package com.braintreepayments.api.shopperinsights

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.braintreepayments.api.core.AnalyticsParamRepository
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.core.TokenizationKey
import com.braintreepayments.api.shopperinsights.ShopperInsightsAnalytics.GET_RECOMMENDED_PAYMENTS_FAILED
import com.braintreepayments.api.shopperinsights.ShopperInsightsAnalytics.GET_RECOMMENDED_PAYMENTS_STARTED
import com.braintreepayments.api.shopperinsights.ShopperInsightsAnalytics.GET_RECOMMENDED_PAYMENTS_SUCCEEDED
import com.braintreepayments.api.shopperinsights.ShopperInsightsAnalytics.PAYPAL_PRESENTED
import com.braintreepayments.api.shopperinsights.ShopperInsightsAnalytics.PAYPAL_SELECTED
import com.braintreepayments.api.shopperinsights.ShopperInsightsAnalytics.VENMO_PRESENTED
import com.braintreepayments.api.shopperinsights.ShopperInsightsAnalytics.VENMO_SELECTED

/**
 * Use [ShopperInsightsClient] to optimize your checkout experience
 * by prioritizing the customer’s preferred payment methods in your UI.
 * By customizing each customer’s checkout experience,
 * you can improve conversion, increase sales/repeat buys and boost user retention/loyalty.
 *
 * Note: **This feature is in beta. It's public API may change in future releases.**
 */
@ExperimentalBetaApi
class ShopperInsightsClient @VisibleForTesting internal constructor(
    private val braintreeClient: BraintreeClient,
    private val analyticsParamRepository: AnalyticsParamRepository = AnalyticsParamRepository.instance,
    private val api: ShopperInsightsApi = ShopperInsightsApi(
        EligiblePaymentsApi(braintreeClient, analyticsParamRepository)
    ),
) {

    /**
     * @param context: an Android context
     * @param authorization: a Tokenization Key or Client Token used to authenticate
     */
    constructor(context: Context, authorization: String) : this(
        BraintreeClient(context, authorization)
    )

    /**
     * Retrieves recommended payment methods based on the provided shopper insights request.
     *
     * @param request The [ShopperInsightsRequest] containing information about the shopper.
     * @return A [ShopperInsightsResult] object indicating the recommended payment methods.
     * Note: This feature is in beta. Its public API may change or be removed in future releases
     * PayPal recommendation is only available for US, AU, FR, DE, ITA, NED, ESP, Switzerland and
     * UK merchants. Venmo recommendation is only available for US merchants.
     */
    fun getRecommendedPaymentMethods(
        request: ShopperInsightsRequest,
        callback: ShopperInsightsCallback
    ) {
        analyticsParamRepository.resetSessionId()
        braintreeClient.sendAnalyticsEvent(GET_RECOMMENDED_PAYMENTS_STARTED)

        if (request.email == null && request.phone == null) {
            callbackFailure(
                callback = callback,
                error = IllegalArgumentException(
                    "One of ShopperInsightsRequest.email or ShopperInsightsRequest.phone must be " +
                        "non-null."
                )
            )
            return
        }

        if (braintreeClient.authorization is TokenizationKey) {
            callbackFailure(
                callback = callback,
                error = BraintreeException(
                    "Invalid authorization. This feature can only be used with a client token."
                )
            )
            return
        }

        api.findEligiblePayments(
            EligiblePaymentsApiRequest(
                request,
                currencyCode = currencyCode,
                countryCode = countryCode,
                accountDetails = includeAccountDetails,
                constraintType = constraintType,
                paymentSources = paymentSources
            ),
            callback = { result, error ->
                handleFindEligiblePaymentsResult(
                    result,
                    error,
                    callback
                )
            }
        )
    }

    private fun handleFindEligiblePaymentsResult(
        result: EligiblePaymentsApiResult?,
        error: Exception?,
        callback: ShopperInsightsCallback
    ) {
        val paypalPaymentMethod = result?.eligibleMethods?.paypal
        val venmoPaymentMethod = result?.eligibleMethods?.venmo
        when {
            error != null -> callbackFailure(callback, error)

            paypalPaymentMethod == null && venmoPaymentMethod == null -> {
                callbackFailure(
                    callback = callback,
                    error = BraintreeException("Required fields missing from API response body")
                )
            }

            else -> {
                callbackSuccess(
                    callback = callback,
                    isEligibleInPayPalNetwork = paypalPaymentMethod?.eligibleInPayPalNetwork == true ||
                        venmoPaymentMethod?.eligibleInPayPalNetwork == true,
                    isPayPalRecommended = paypalPaymentMethod?.recommended == true,
                    isVenmoRecommended = venmoPaymentMethod?.recommended == true
                )
            }
        }
    }

    private fun callbackFailure(
        callback: ShopperInsightsCallback,
        error: Exception
    ) {
        braintreeClient.sendAnalyticsEvent(GET_RECOMMENDED_PAYMENTS_FAILED)
        callback.onResult(ShopperInsightsResult.Failure(error))
    }

    private fun callbackSuccess(
        callback: ShopperInsightsCallback,
        isEligibleInPayPalNetwork: Boolean,
        isPayPalRecommended: Boolean,
        isVenmoRecommended: Boolean,
    ) {
        braintreeClient.sendAnalyticsEvent(GET_RECOMMENDED_PAYMENTS_SUCCEEDED)
        callback.onResult(
            ShopperInsightsResult.Success(
                ShopperInsightsInfo(
                    isEligibleInPayPalNetwork,
                    isPayPalRecommended,
                    isVenmoRecommended
                )
            )
        )
    }

    /**
     * Call this method when the PayPal button has been successfully displayed to the buyer.
     * This method sends analytics to help improve the Shopper Insights feature experience.
     */
    fun sendPayPalPresentedEvent() {
        braintreeClient.sendAnalyticsEvent(PAYPAL_PRESENTED)
    }

    /**
     * Call this method when the PayPal button has been selected/tapped by the buyer.
     * This method sends analytics to help improve the Shopper Insights feature experience.
     */
    fun sendPayPalSelectedEvent() {
        braintreeClient.sendAnalyticsEvent(PAYPAL_SELECTED)
    }

    /**
     * Call this method when the Venmo button has been successfully displayed to the buyer.
     * This method sends analytics to help improve the Shopper Insights feature experience.
     */
    fun sendVenmoPresentedEvent() {
        braintreeClient.sendAnalyticsEvent(VENMO_PRESENTED)
    }

    /**
     * Call this method when the Venmo button has been selected/tapped by the buyer.
     * This method sends analytics to help improve the Shopper Insights feature experience.
     */
    fun sendVenmoSelectedEvent() {
        braintreeClient.sendAnalyticsEvent(VENMO_SELECTED)
    }

    companion object {
        // Default values
        private const val countryCode = "US"
        private const val currencyCode = "USD"
        private const val constraintType = "INCLUDE"
        private val paymentSources = listOf("PAYPAL", "VENMO")
        private const val includeAccountDetails = true
    }
}
