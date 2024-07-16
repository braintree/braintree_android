package com.braintreepayments.api.core

import com.braintreepayments.api.sharedutils.HttpResponseTiming

internal fun interface ConfigurationLoaderCallback {
    fun onResult(result: Configuration?, error: Exception?, timing: HttpResponseTiming?)
}
