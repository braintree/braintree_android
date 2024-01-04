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
    private val paymentReadyAPI: PaymentReadyApi,
    private val braintreeClient: BraintreeClient,
    private val deviceInspector: DeviceInspector
) {
    constructor(braintreeClient: BraintreeClient) : this(
        PaymentReadyApi(),
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

        paymentReadyAPI.processRequest(request)
        // Hardcoded result
        callback.onResult(
            ShopperInsightsResult.Success(
                ShopperInsightsInfo(
                    isPayPalRecommended = false,
                    isVenmoRecommended = false
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
}
