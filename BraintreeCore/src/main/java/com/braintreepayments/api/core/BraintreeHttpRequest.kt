package com.braintreepayments.api.core

import androidx.annotation.RestrictTo
import com.braintreepayments.api.sharedutils.HttpClient
import com.braintreepayments.api.sharedutils.HttpClient.RetryStrategy

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class BraintreeHttpRequest(
    val method: String,
    val path: String,
    val data: String? = null,
    val additionalHeaders: Map<String, String> = emptyMap(),
    val configuration: Configuration? = null,
    val authorization: Authorization? = null,
    @RetryStrategy val retryStrategy: Int = HttpClient.NO_RETRY
)
