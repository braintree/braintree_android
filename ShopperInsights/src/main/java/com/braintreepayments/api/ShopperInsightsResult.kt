package com.braintreepayments.api

/**
 * The result object returned when insights about a shopper is requested.
 */
sealed class ShopperInsightsResult {

    /**
     * @property response The response object describing the shopper's insights.
     */
    class Success(val response: ShopperInsightsInfo) : ShopperInsightsResult()

    /**
     * @property error An object that describes the error that occurred.
     */
    class Failure(val error: Exception) : ShopperInsightsResult()
}
