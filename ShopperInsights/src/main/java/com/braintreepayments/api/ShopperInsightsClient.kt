package com.braintreepayments.api

import androidx.annotation.VisibleForTesting
import com.braintreepayments.api.ShopperInsightsAnalytics.GET_RECOMMENDED_PAYMENTS_FAILED
import com.braintreepayments.api.ShopperInsightsAnalytics.GET_RECOMMENDED_PAYMENTS_STARTED
import com.braintreepayments.api.ShopperInsightsAnalytics.GET_RECOMMENDED_PAYMENTS_SUCCEEDED
import com.braintreepayments.api.ShopperInsightsAnalytics.PAYPAL_PRESENTED
import com.braintreepayments.api.ShopperInsightsAnalytics.PAYPAL_SELECTED

/**
 * Use [ShopperInsightsClient] to optimize your checkout experience
 * by prioritizing the customer’s preferred payment methods in your UI.
 * By customizing each customer’s checkout experience,
 * you can improve conversion, increase sales/repeat buys and boost user retention/loyalty.
 *
 * Note: **This feature is in beta. It's public API may change in future releases.**
 */
class ShopperInsightsClient @VisibleForTesting internal constructor(
    private val api: ShopperInsightsApi,
    private val braintreeClient: BraintreeClient
) {
    constructor(braintreeClient: BraintreeClient) : this(
        ShopperInsightsApi(EligiblePaymentsApi(braintreeClient)),
        braintreeClient,
    )

    /**
     * Retrieves recommended payment methods based on the provided shopper insights request.
     *
     * @param context Android context
     * @param request The [ShopperInsightsRequest] containing information about the shopper.
     * @return A [ShopperInsightsResult] object indicating the recommended payment methods.
     */
    fun getRecommendedPaymentMethods(
        request: ShopperInsightsRequest,
        callback: ShopperInsightsCallback
    ) {
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

        // TODO: get correct merchant ID from SDK
        val merchantId = "MXSJ4F5BADVNS"

        api.findEligiblePayments(
            EligiblePaymentsApiRequest(
                request,
                merchantId = merchantId,
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
        when {
            error != null -> callbackFailure(callback, error)

            result?.eligibleMethods?.paypal == null -> {
                callbackFailure(
                    callback = callback,
                    error = BraintreeException("Required fields missing from API response body")
                )
            }

            else -> {
                callbackSuccess(
                    callback = callback,
                    isPayPalRecommended = isPaymentRecommended(result.eligibleMethods.paypal)
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
        isPayPalRecommended: Boolean
    ) {
        braintreeClient.sendAnalyticsEvent(GET_RECOMMENDED_PAYMENTS_SUCCEEDED)
        callback.onResult(
            ShopperInsightsResult.Success(
                ShopperInsightsInfo(isPayPalRecommended)
            )
        )
    }

    private fun isPaymentRecommended(paymentDetail: EligiblePaymentMethodDetails?): Boolean {
        return paymentDetail?.eligibleInPayPalNetwork == true && paymentDetail.recommended
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

    companion object {
        // Default values
        private const val countryCode = "US"
        private const val currencyCode = "USD"
        private const val constraintType = "INCLUDE"
        private val paymentSources = listOf("PAYPAL")
        private const val includeAccountDetails = true
    }
}
