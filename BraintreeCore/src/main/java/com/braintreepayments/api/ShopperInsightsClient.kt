package com.braintreepayments.api

import androidx.annotation.VisibleForTesting

/**
 * Use [ShopperInsightsClient] to optimize your checkout experience
 * by prioritizing the customer’s preferred payment methods in your UI.
 * By customizing each customer’s checkout experience,
 * you can improve conversion, increase sales/repeat buys and boost user retention/loyalty.
 * - Note: This feature is in beta. It's public API may change in future releases.
 */
class ShopperInsightsClient @VisibleForTesting internal constructor() {

    /**
     * Retrieves recommended payment methods based on the provided shopper insights request.
     *
     * This function takes a `ShopperInsightRequest` object as input and returns a `ShopperInsightResult`.
     * The result includes flags indicating whether specific payment methods,
     * such as PayPal or Venmo, are potentially a high priority for the shopper.
     *
     * @param request The `ShopperInsightRequest` containing information about the shopper.
     * @return A `ShopperInsightResult` object indicating the recommended payment methods.
     */
    @Suppress("UnusedPrivateMember")
    fun getRecommendedPaymentMethods(
        request: ShopperInsightRequest,
        callback: ShopperInsightCallback
    ) {
        // TODO: - Add isAppInstalled checks for PP & Venmo. DTBTSDK-3176
        // TODO: - Make API call to PaymentReadyAPI. DTBTSDK-3176
        // Hardcoded result
        callback.onResult(
            ShopperInsightResult.Success(
                ShopperInsightResponse(
                    isPayPalRecommended = false,
                    isVenmoRecommended = false
                )
            )
        )
    }
}
