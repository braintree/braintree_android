package com.braintreepayments.api.core

import com.braintreepayments.api.sharedutils.HttpResponseTiming

/**
 * Result of calling [ConfigurationLoader.loadConfiguration]
 */
internal sealed class ConfigurationLoaderResult {

    class Success(
        val configuration: Configuration,
        val timing: HttpResponseTiming? = null
    ) : ConfigurationLoaderResult()

    class Failure(val error: Exception) : ConfigurationLoaderResult()
}