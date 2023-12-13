package com.braintreepayments.api

/**
 * Callback for receiving result of
 * [AmericanExpressClient.getRewardsBalance].
 */
fun interface AmericanExpressGetRewardsBalanceCallback {
    /**
     * @param americanExpressResult the [AmericanExpressResult]
     */
    fun onAmericanExpressResult(americanExpressResult: AmericanExpressResult?)
}