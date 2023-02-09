package com.braintreepayments.api

internal interface ConfigurationLoaderCallback {
    fun onResult(result: ConfigurationLoaderResult?, error: Exception?)
}