package com.braintreepayments.api.americanexpress

/**
 * Result of fetching American Express rewards balance
 */
sealed class AmericanExpressResult {

    /**
     * The [rewardsBalance] was successfully fetched
     */
    class Success internal constructor(
        val rewardsBalance: AmericanExpressRewardsBalance
    ) : AmericanExpressResult()

    /**
     * There was an [error] fetching rewards balance
     */
    class Failure internal constructor(val error: Exception) : AmericanExpressResult()
}
