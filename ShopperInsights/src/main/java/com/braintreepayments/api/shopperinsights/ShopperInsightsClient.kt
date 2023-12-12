package com.braintreepayments.api.shopperinsights

import androidx.annotation.VisibleForTesting

/**
 * Use [ShopperInsightsClient] to optimize your checkout experience
 * by prioritizing the customer’s preferred payment methods in your UI.
 * By customizing each customer’s checkout experience,
 * you can improve conversion, increase sales/repeat buys and boost user retention/loyalty.
 *
 * Note: **This feature is in beta. It's public API may change in future releases.**
 */
class ShopperInsightsClient @VisibleForTesting internal constructor(
    private val paymentReadyAPI: PaymentReadyApi
) {
    /**
     * Retrieves recommended payment methods based on the provided shopper insights request.
     *
     * @param request The [ShopperInsightsRequest] containing information about the shopper.
     * @return A [ShopperInsightsResult] object indicating the recommended payment methods.
     */
    fun getRecommendedPaymentMethods(
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

        // TODO: - Add isAppInstalled checks for PP & Venmo. DTBTSDK-3176
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
}
