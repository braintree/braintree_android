package com.braintreepayments.api.core

import com.braintreepayments.api.sharedutils.HttpResponseTiming

data class ConfigurationLoaderResponse(
    val configuration: Configuration? = null,
    val error: Exception? = null,
    val timing: HttpResponseTiming? = null
)
