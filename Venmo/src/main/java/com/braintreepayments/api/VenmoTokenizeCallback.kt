package com.braintreepayments.api

/**
 * Used to receive the result of [VenmoClient.tokenize]
 */
fun interface VenmoTokenizeCallback {

    fun onVenmoResult(result: VenmoResult)
}
