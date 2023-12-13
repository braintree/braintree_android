package com.braintreepayments.api

/**
 * Callback for receiving result of
 * [AmericanExpressClient.getRewardsBalance].
 */
interface AmericanExpressGetRewardsBalanceCallback {
    /**
     * @param americanExpressResult the [AmericanExpressResult]
     */
    fun onResult(americanExpressResult: AmericanExpressResult?)
}