package com.braintreepayments.api.uicomponents

/**
 * Callback for receiving result of [PayPalButton] click and launch attempt.
 */
fun interface PayPalButtonLaunchCallback {
    /**
     * @param result the pending request or failure result from launching the PayPal flow
     */
    fun onLaunch(result: PayPalButtonPendingRequest)
}