package com.braintreepayments.api.sepadirectdebit

internal fun interface CreateMandateCallback {
    fun onResult(result: CreateMandateResult?, error: Exception?)
}
