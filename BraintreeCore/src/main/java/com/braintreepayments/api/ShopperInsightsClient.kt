package com.braintreepayments.api

import androidx.annotation.VisibleForTesting

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
        val jsonBody = when (request) {
            is ShopperInsightRequest.Email -> createEmailJsonBody(request.email)
            is ShopperInsightRequest.Phone -> createPhoneJsonBody(
                request.phone
            )
            is ShopperInsightRequest.EmailAndPhone -> createEmailPhoneJsonBody(
                request.email,
                request.phone
            )
        }
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

    private fun createEmailJsonBody(email: String): String {
        return "{\"customer\": {\"email\": \"${email}\"}}"
    }

    private fun createPhoneJsonBody(phone: BuyerPhone): String {
        return "{\"customer\": {\"phone\": {\"countryCode\": \"${phone.countryCode}\", \"nationalNumber\": \"${phone.nationalNumber}\"}}}"
    }

    private fun createEmailPhoneJsonBody(email: String, phone: BuyerPhone): String {
        return "{\"customer\": {" +
                "\"email\": \"${email}\"," +
                "\"phone\": " +
                "{\"countryCode\": \"${phone.countryCode}\", " +
                "\"nationalNumber\": \"${phone.nationalNumber}\"}" +
                "}" +
                "}"
    }
}
