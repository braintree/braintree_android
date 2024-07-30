package com.braintreepayments.api.paypal

internal interface PayPalInternalClientCallback {

    fun onResult(
        paymentAuthRequest: PayPalPaymentAuthRequestParams?,
        error: Exception?
    )
}
