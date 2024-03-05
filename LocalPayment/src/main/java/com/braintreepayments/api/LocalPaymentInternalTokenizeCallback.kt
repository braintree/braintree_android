package com.braintreepayments.api

internal fun interface LocalPaymentInternalTokenizeCallback {

    fun onResult(localPaymentNonce: LocalPaymentNonce?, error: Exception?)
}
