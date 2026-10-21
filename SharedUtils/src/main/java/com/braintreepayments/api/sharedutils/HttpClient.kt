package com.braintreepayments.api.sharedutils

import androidx.annotation.RestrictTo
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class HttpClient internal constructor(
    private val okHttpSynchronousHttpClient: OkHttpSynchronousHttpClient = OkHttpSynchronousHttpClient(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    constructor() : this(
        okHttpSynchronousHttpClient = OkHttpSynchronousHttpClient(),
        ioDispatcher = Dispatchers.IO,
    )
    /**
    * @throws Exception if the network request fails
    */
    suspend fun sendRequest(request: OkHttpRequest): HttpResponse {
        return withContext(ioDispatcher) {
            okHttpSynchronousHttpClient.executeRequest(request)
        }
    }

    /**
     * Sends [request] and returns the raw response body, for binary payloads such as images.
     *
     * @param maxBodyBytes Limit on the response body size, 5 MB by default, enforced even if the response omits
     * `Content-Length` (e.g. chunked transfer).
     * @throws java.io.IOException if the network request fails, the response is unsuccessful or has no body, or the
     * body size exceeds [maxBodyBytes]
     * @throws IllegalArgumentException if the request URL is not a valid http or https URL
     */
    suspend fun sendRequestForBytes(
        request: OkHttpRequest,
        maxBodyBytes: Long = OkHttpSynchronousHttpClient.DEFAULT_MAX_BODY_BYTES
    ): ByteArray {
        return withContext(ioDispatcher) {
            okHttpSynchronousHttpClient.executeRequestForBytes(request, maxBodyBytes)
        }
    }
}
