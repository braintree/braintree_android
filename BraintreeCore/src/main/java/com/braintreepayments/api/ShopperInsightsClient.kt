package com.braintreepayments.api

import androidx.annotation.VisibleForTesting
import org.json.JSONObject

/**
 * Use [ShopperInsightsClient] to optimize your checkout experience
 * by prioritizing the customer’s preferred payment methods in your UI.
 * By customizing each customer’s checkout experience,
 * you can improve conversion, increase sales/repeat buys and boost user retention/loyalty.
 *
 * Note: This feature is in beta. It's public API may change in future releases.
 */
class ShopperInsightsClient @VisibleForTesting internal constructor(
    private val paymentReadyAPI: PaymentReadyAPI
) {

    /**
     * Retrieves recommended payment methods based on the provided shopper insights request.
     *
     * @param request The [ShopperInsightRequest] containing information about the shopper.
     * @return A [ShopperInsightResult] object indicating the recommended payment methods.
     */
    @Suppress("UnusedPrivateMember")
    fun getRecommendedPaymentMethods(
        request: ShopperInsightRequest,
        callback: ShopperInsightCallback
    ) {
        val jsonBody = request.toJson()
        // TODO: - Add isAppInstalled checks for PP & Venmo. DTBTSDK-3176
        paymentReadyAPI.processRequest(jsonBody)
        // Hardcoded result
        callback.onResult(
            ShopperInsightResult.Success(
                ShopperInsightInfo(
                    isPayPalRecommended = false,
                    isVenmoRecommended = false
                )
            )
        )
    }
}
