package com.braintreepayments.americanexpress

/**
 * Result of fetching American Express rewards balance
 */
sealed class AmericanExpressResult {

    /**
     * The [rewardsBalance] was successfully fetched
     */
    class Success(val rewardsBalance: AmericanExpressRewardsBalance) : AmericanExpressResult()

    /**
     * There was an [error] fetching rewards balance
     */
    class Failure(val error: Exception) : AmericanExpressResult()
}
