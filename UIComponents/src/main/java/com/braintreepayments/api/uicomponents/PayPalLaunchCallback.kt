package com.braintreepayments.api.uicomponents

import com.braintreepayments.api.paypal.PayPalPendingRequest

/**
 * Callback for receiving result of PayPal payment authorization request from PayPalButton.
 */
fun interface PayPalLaunchCallback {

    /**
     * @param payPalPendingRequest a request used to launch the PayPal payment authorization flow
     */
    fun onPayPalPaymentAuthRequest(payPalPendingRequest: PayPalPendingRequest)
}
