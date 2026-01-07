package com.braintreepayments.api.sharedutils

import androidx.annotation.RestrictTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class HttpClient internal constructor(
    private val okHttpSynchronousHttpClient: OkHttpSynchronousHttpClient = OkHttpSynchronousHttpClient(),
) {

    constructor() : this(
        okHttpSynchronousHttpClient = OkHttpSynchronousHttpClient(),
    )
    /**
    * @throws Exception if the network request fails
    */
    suspend fun sendRequest(request: OkHttpRequest): HttpResponse {
        return withContext(Dispatchers.IO) {
            okHttpSynchronousHttpClient.executeRequest(request)
        }
    }
}
