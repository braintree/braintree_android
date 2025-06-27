package com.braintreepayments.api.shopperinsights.v2

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Represents the result of customer recommendations generation.
 */
@ExperimentalBetaApi
sealed class CustomerRecommendationsResult {
    /**
     * Indicates a successful customer recommendations generation.
     *
     * @property customerRecommendations The customer recommendations generated.
     */
    class Success internal constructor(
        val customerRecommendations: CustomerRecommendations
    ) : CustomerRecommendationsResult()

    /**
     * Indicates a failure during customer recommendations generation.
     *
     * @property error The exception that caused the failure.
     */
    class Failure internal constructor(val error: Exception) : CustomerRecommendationsResult()
}
