package com.braintreepayments.api

/**
 * A callback that returns information on whether someone is a PayPal or a Venmo shopper.
 */
@UnstableApi
fun interface ShopperInsightsCallback {
    fun onResult(result: ShopperInsightsResult)
}
