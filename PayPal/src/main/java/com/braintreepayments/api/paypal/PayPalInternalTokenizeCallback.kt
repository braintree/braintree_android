package com.braintreepayments.api.paypal

internal fun interface PayPalInternalTokenizeCallback {

    fun onResult(payPalAccountNonce: PayPalAccountNonce?, error: Exception?)
}
