package com.braintreepayments.api.uicomponents

import com.braintreepayments.api.paypal.PayPalResult

/**
 * Callback for receiving the result of the PayPal payment flow
 * after the user returns to the app.
 */
fun interface PayPalButtonResultCallback {

    /**
     * Called when the PayPal flow completes after user returns to app.
     *
     * @param result The [PayPalResult] containing success, failure, or cancellation.
     */
    fun onResult(result: PayPalResult)
}
