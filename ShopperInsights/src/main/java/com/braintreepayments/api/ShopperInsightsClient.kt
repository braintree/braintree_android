package com.braintreepayments.api

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.braintreepayments.api.ShopperInsightsAnalytics.GET_RECOMMENDED_PAYMENTS_FAILED
import com.braintreepayments.api.ShopperInsightsAnalytics.GET_RECOMMENDED_PAYMENTS_STARTED
import com.braintreepayments.api.ShopperInsightsAnalytics.GET_RECOMMENDED_PAYMENTS_SUCCEEDED
import com.braintreepayments.api.ShopperInsightsAnalytics.PAYPAL_PRESENTED
import com.braintreepayments.api.ShopperInsightsAnalytics.PAYPAL_SELECTED
import com.braintreepayments.api.ShopperInsightsAnalytics.VENMO_PRESENTED
import com.braintreepayments.api.ShopperInsightsAnalytics.VENMO_SELECTED

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
    private val braintreeClient: BraintreeClient,
    private val deviceInspector: DeviceInspector
) {
    constructor(braintreeClient: BraintreeClient) : this(
        ShopperInsightsApi(EligiblePaymentsApi(braintreeClient)),
        braintreeClient,
        DeviceInspector()
    )

    /**
     * Retrieves recommended payment methods based on the provided shopper insights request.
     *
     * @param context Android context
     * @param request The [ShopperInsightsRequest] containing information about the shopper.
     * @return A [ShopperInsightsResult] object indicating the recommended payment methods.
     */
    @Suppress("LongMethod")
    fun getRecommendedPaymentMethods(
        context: Context,
        request: ShopperInsightsRequest,
        callback: ShopperInsightsCallback
    ) {
        braintreeClient.sendAnalyticsEvent(GET_RECOMMENDED_PAYMENTS_STARTED)
        if (request.email == null && request.phone == null) {
            callback.onResult(
                ShopperInsightsResult.Failure(
                    IllegalArgumentException(
                        "One of ShopperInsightsRequest.email or " +
                            "ShopperInsightsRequest.phone must be non-null."
                    )
                )
            )
            braintreeClient.sendAnalyticsEvent(GET_RECOMMENDED_PAYMENTS_FAILED)
            return
        }

        val applicationContext = context.applicationContext
        val isVenmoAppInstalled = deviceInspector.isVenmoInstalled(applicationContext)
        val isPayPalAppInstalled = deviceInspector.isPayPalInstalled(applicationContext)

        if (isVenmoAppInstalled && isPayPalAppInstalled) {
            callback.onResult(
                ShopperInsightsResult.Success(
                    ShopperInsightsInfo(
                        isPayPalRecommended = true,
                        isVenmoRecommended = true
                    )
                )
            )
            braintreeClient.sendAnalyticsEvent(GET_RECOMMENDED_PAYMENTS_SUCCEEDED)
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
                if (error == null) {
                    callback.onResult(
                        ShopperInsightsResult.Success(
                            ShopperInsightsInfo(
                                isPayPalRecommended = isPaymentRecommended(result?.eligibleMethods?.paypal),
                                isVenmoRecommended = isPaymentRecommended(result?.eligibleMethods?.venmo)
                            )
                        )
                    )
                } else {
                    callback.onResult(
                        ShopperInsightsResult.Failure(
                            error
                        )
                    )
                }
            }
        )
        braintreeClient.sendAnalyticsEvent(GET_RECOMMENDED_PAYMENTS_SUCCEEDED)
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
