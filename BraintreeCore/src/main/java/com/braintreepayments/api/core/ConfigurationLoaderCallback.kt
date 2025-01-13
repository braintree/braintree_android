package com.braintreepayments.api.core

internal fun interface ConfigurationLoaderCallback {
    fun onResult(result: ConfigurationLoaderResult)
}
