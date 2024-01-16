package com.braintreepayments.api

import ShopperInsightApiResult
import com.braintreepayments.api.ShopperInsightsApiRequest.Companion.toJson

internal class FindEligiblePaymentsApi {
    fun execute(request: ShopperInsightsApiRequest): ShopperInsightApiResult {
        request.toJson()
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
