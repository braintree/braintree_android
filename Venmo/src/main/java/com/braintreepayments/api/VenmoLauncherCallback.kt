package com.braintreepayments.api

/**
 * Used to receive notification that the Venmo payment authorization flow completed
 * Once this is invoked, continue the flow by calling
 * [VenmoClient.tokenize]
 */
fun interface VenmoLauncherCallback {
    fun onVenmoPaymentAuthResult(venmoPaymentAuthResult: VenmoPaymentAuthResult)
}