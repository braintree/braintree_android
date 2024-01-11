package com.braintreepayments.api

import ShopperInsightApiResult

internal class ShopperInsightsApi(
    private val shoppingInsightsCreateBody: ShoppingInsightsCreateBody
) {
    fun execute(request: ShopperInsightsApiRequest): ShopperInsightApiResult {
        shoppingInsightsCreateBody.execute(request)
        // TODO: Network call

        // Hardcoded result
        return ShopperInsightApiResult(
            eligibleMethods = ShopperInsightsPaymentMethods(
                paypal = ShopperInsightsPaymentMethodDetails(
                    canBeVaulted = true,
                    eligibleInPayPalNetwork = true,
                    recommended = true,
                    recommendedPriority = 1
                ),
                venmo = ShopperInsightsPaymentMethodDetails(
                    canBeVaulted = true,
                    eligibleInPayPalNetwork = true,
                    recommended = true,
                    recommendedPriority = 1
                )
            )
        )
    }
}
