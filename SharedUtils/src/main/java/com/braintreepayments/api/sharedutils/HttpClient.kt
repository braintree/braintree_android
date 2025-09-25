package com.braintreepayments.api.sharedutils

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class HttpClient internal constructor(
    private val okHttpSynchronousHttpClient: OkHttpSynchronousHttpClient = OkHttpSynchronousHttpClient(),
    private val scheduler: Scheduler = ThreadScheduler()
) {

    constructor() : this(
        okHttpSynchronousHttpClient = OkHttpSynchronousHttpClient(),
        scheduler = ThreadScheduler()
    )

    @Suppress("TooGenericExceptionCaught")
    fun sendRequest(
        request: OkHttpRequest,
        callback: NetworkResponseCallback?,
    ) {
        scheduler.runOnBackground {
            try {
                val httpResponse = okHttpSynchronousHttpClient.executeRequest(request)
                callback?.let {
                    scheduler.runOnMain { callback.onResult(NetworkResponseCallback.Result.Success(httpResponse)) }
                }
            } catch (e: Exception) {
                notifyErrorOnMainThread(callback, e)
            }
        }
    }

    private fun notifyErrorOnMainThread(callback: NetworkResponseCallback?, e: Exception) {
        if (callback != null) {
            scheduler.runOnMain { callback.onResult(NetworkResponseCallback.Result.Failure(e)) }
        }
    }
}
