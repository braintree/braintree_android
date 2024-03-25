package com.braintreepayments.api

internal fun interface VenmoApiCallback {

    fun onResult(paymentContextId: String?, exception: Exception?)
}
