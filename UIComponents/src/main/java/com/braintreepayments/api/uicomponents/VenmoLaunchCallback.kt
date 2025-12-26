package com.braintreepayments.api.uicomponents

import com.braintreepayments.api.venmo.VenmoPendingRequest

/**
 * Callback for receiving result of Venmo payment authorization request from VenmoButton.
 */
fun interface VenmoLaunchCallback {

    fun onVenmoPaymentAuthRequest(venmoPendingRequest: VenmoPendingRequest)
}
