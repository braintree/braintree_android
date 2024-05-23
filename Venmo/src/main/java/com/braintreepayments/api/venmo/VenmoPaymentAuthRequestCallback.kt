package com.braintreepayments.api.venmo

/**
 * Callback to handle result from [VenmoClient.tokenize]
 */
fun interface VenmoPaymentAuthRequestCallback {

    fun onVenmoPaymentAuthRequest(venmoPaymentAuthRequest: VenmoPaymentAuthRequest)
}
