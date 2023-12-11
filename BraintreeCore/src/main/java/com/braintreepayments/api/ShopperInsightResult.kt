package com.braintreepayments.api

/**
 * The result object returned when insights about a shopper is requested.
 */
sealed class ShopperInsightResult {

    /**
     * @property response The response object describing the shopper's insights.
     */
    class Success(val response: ShopperInsightInfo) : ShopperInsightResult()

    /**
     * @property error An object that describes the error that occurred.
     */
    class Failure(val error: Exception) : ShopperInsightResult()
}
