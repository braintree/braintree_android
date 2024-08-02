package com.braintreepayments.api.paypal

internal fun interface PayPalInternalClientCallback {

    fun onResult(
        paymentAuthRequest: PayPalPaymentAuthRequestParams?,
        error: Exception?
    )
}
