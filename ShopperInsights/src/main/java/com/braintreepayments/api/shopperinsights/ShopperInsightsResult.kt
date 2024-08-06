package com.braintreepayments.api.shopperinsights

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * The result object returned when insights about a shopper is requested.
 */
sealed class ShopperInsightsResult {

    /**
     * @property response The response object describing the shopper's insights.
     */
    @OptIn(ExperimentalBetaApi::class)
    class Success(val response: ShopperInsightsInfo) : ShopperInsightsResult()

    /**
     * @property error An object that describes the error that occurred.
     */
    class Failure(val error: Exception) : ShopperInsightsResult()
}
