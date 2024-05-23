package com.braintreepayments.api.venmo

internal fun interface VenmoApiCallback {

    fun onResult(paymentContextId: String?, exception: Exception?)
}
