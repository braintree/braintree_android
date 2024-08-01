package com.braintreepayments.api.paypal

internal interface PayPalInternalTokenizeCallback {

    fun onResult(payPalAccountNonce: PayPalAccountNonce?, error: Exception?)
}
