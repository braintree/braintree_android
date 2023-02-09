package com.braintreepayments.api

import java.lang.Exception

internal data class ConfigurationLoaderResult @JvmOverloads constructor(
    val configuration: Configuration,
    val loadFromCacheError: Exception? = null,
    val saveToCacheError: Exception? = null
)