package com.braintreepayments.api

/**
 * Callback to handle result from [VenmoClient.tokenize]
 */
fun interface VenmoPaymentAuthRequestCallback {

    fun onVenmoPaymentAuthRequest(venmoPaymentAuthRequest: VenmoPaymentAuthRequest)
}
