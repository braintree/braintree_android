package com.braintreepayments.api

internal fun interface CardinalInitializeCallback {
    fun onResult(consumerSessionId: String?, error: Exception?)
}
