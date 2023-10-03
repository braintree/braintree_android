package com.braintreepayments.api

internal fun interface ConfigurationLoaderCallback {
    fun onResult(result: Configuration?, error: Exception?)
}
