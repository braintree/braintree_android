package com.braintreepayments.api.venmo

/**
 * Callback to handle result from [VenmoClient.createPaymentAuthRequest].
 */
fun interface VenmoPaymentAuthRequestCallback {

    fun onVenmoPaymentAuthRequest(venmoPaymentAuthRequest: VenmoPaymentAuthRequest)
}
