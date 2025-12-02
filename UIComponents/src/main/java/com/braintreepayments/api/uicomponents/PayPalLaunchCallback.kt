package com.braintreepayments.api.uicomponents

import com.braintreepayments.api.paypal.PayPalPendingRequest

fun interface PayPalLaunchCallback {

    fun onPayPalPaymentAuthRequest(payPalPendingRequest: PayPalPendingRequest)

}