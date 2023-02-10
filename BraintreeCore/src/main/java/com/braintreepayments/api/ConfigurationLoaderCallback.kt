package com.braintreepayments.api

internal interface ConfigurationLoaderCallback {
    fun onResult(result: Configuration?, error: Exception?)
}