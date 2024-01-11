package com.braintreepayments.api

import android.content.Context
import androidx.annotation.VisibleForTesting
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
    private val shoppingInsightsApi: ShopperInsightsApi,
    private val braintreeClient: BraintreeClient,
    private val deviceInspector: DeviceInspector
) {
    constructor(braintreeClient: BraintreeClient) : this(
        ShopperInsightsApi(
            ShoppingInsightsCreateBody()
        ),
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
    fun getRecommendedPaymentMethods(
        context: Context,
        request: ShopperInsightsRequest,
        callback: ShopperInsightsCallback
    ) {
        if (request.email == null && request.phone == null) {
            callback.onResult(
                ShopperInsightsResult.Failure(
                    IllegalArgumentException(
                        "One of ShopperInsightsRequest.email or " +
                            "ShopperInsightsRequest.phone must be non-null."
                    )
                )
            )
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
            return
        }

        // TODO: get correct merchant ID from SDK
        val merchantId = "MXSJ4F5BADVNS"

        // Default values
        val countryCode = "US"
        val currencyCode = "USD"
        val constraintType = "INCLUDE"
        val paymentSources = listOf("PAYPAL", "VENMO")
        val includeAccountDetails = true

        val result = shoppingInsightsApi.execute(
            ShopperInsightsApiRequest(
                request,
                merchantId = merchantId,
                currencyCode = currencyCode,
                countryCode = countryCode,
                accountDetails = includeAccountDetails,
                constraintType = constraintType,
                paymentSources = paymentSources
            )
        )
        // Hardcoded result
        callback.onResult(
            ShopperInsightsResult.Success(
                ShopperInsightsInfo(
                    isPayPalRecommended = isPaymentRecommended(result.eligibleMethods.paypal),
                    isVenmoRecommended = isPaymentRecommended(result.eligibleMethods.venmo)
                )
            )
        )
    }

    private fun isPaymentRecommended(paymentDetail: ShopperInsightsPaymentMethodDetails): Boolean {
        return paymentDetail.eligibleInPayPalNetwork && paymentDetail.recommended
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
}
