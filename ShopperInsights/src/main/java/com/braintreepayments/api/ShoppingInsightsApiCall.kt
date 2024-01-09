package com.braintreepayments.api

import PaymentMethodDetails
import PaymentMethods
import ShopperInsightApiResult

internal class ShoppingInsightsApiCall(
    private val shoppingInsightsCreateBody: ShoppingInsightsCreateBody
) {
    fun execute(request: ShopperInsightsApiRequest): ShopperInsightApiResult {
        shoppingInsightsCreateBody.execute(request)
        // TODO: Network call

        // Hardcoded result
        return ShopperInsightApiResult(
            eligibleMethods = PaymentMethods(
                paypal = PaymentMethodDetails(
                    canBeVaulted = true,
                    eligibleInPaypalNetwork = true,
                    recommended = true,
                    recommendedPriority = 1
                ),
                venmo = PaymentMethodDetails(
                    canBeVaulted = true,
                    eligibleInPaypalNetwork = true,
                    recommended = true,
                    recommendedPriority = 1
                )
            )
        )
    }
}
