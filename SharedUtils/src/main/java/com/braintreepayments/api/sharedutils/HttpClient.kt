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
}
