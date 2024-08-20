package com.braintreepayments.api.core

import androidx.annotation.RestrictTo
import com.braintreepayments.api.sharedutils.HttpClient
import com.braintreepayments.api.sharedutils.HttpClient.RetryStrategy
import com.braintreepayments.api.sharedutils.HttpMethod

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class InternalHttpRequest(
    val method: HttpMethod,
    val path: String,
    val data: String? = null,
    val additionalHeaders: Map<String, String> = emptyMap(),
    @RetryStrategy val retryStrategy: Int = HttpClient.NO_RETRY
)
