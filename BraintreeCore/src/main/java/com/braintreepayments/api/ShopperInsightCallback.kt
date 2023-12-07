package com.braintreepayments.api

/**
 * A callback that returns information on whether someone is a PayPal/Venmo shopper.
 */
fun interface ShopperInsightCallback {
    fun onResult(result: ShopperInsightResult)
}
