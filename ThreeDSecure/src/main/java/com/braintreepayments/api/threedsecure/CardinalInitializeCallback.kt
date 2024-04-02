package com.braintreepayments.api.threedsecure

internal fun interface CardinalInitializeCallback {
    fun onResult(consumerSessionId: String?, error: Exception?)
}
