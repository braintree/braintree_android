package com.braintreepayments.api.core

import com.braintreepayments.api.sharedutils.HttpResponseTiming

/**
 * Result of calling [ConfigurationLoader.loadConfiguration]
 */
internal sealed class ConfigurationLoaderResult {

    data class Success(
        val configuration: Configuration,
        val timing: HttpResponseTiming? = null
    ) : ConfigurationLoaderResult()

    data class Failure(val error: Exception) : ConfigurationLoaderResult()
}
