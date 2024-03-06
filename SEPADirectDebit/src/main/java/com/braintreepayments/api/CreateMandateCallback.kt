package com.braintreepayments.api

internal fun interface CreateMandateCallback {
    fun onResult(result: CreateMandateResult?, error: Exception?)
}
