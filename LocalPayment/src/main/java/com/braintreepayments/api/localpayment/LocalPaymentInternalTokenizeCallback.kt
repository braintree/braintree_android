package com.braintreepayments.api.localpayment

internal fun interface LocalPaymentInternalTokenizeCallback {

    fun onResult(localPaymentNonce: LocalPaymentNonce?, error: Exception?)
}
