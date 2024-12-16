package com.braintreepayments.api.sharedutils

import androidx.annotation.RestrictTo
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import javax.net.ssl.SSLSocketFactory

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class HttpClient internal constructor(
    private val syncHttpClient: SynchronousHttpClient,
    private val scheduler: Scheduler
) {
    enum class RetryStrategy { NO_RETRY, RETRY_MAX_3_TIMES }

    private val retryCountMap: MutableMap<URL, Int> = HashMap()

    constructor(
        socketFactory: SSLSocketFactory,
        httpResponseParser: HttpResponseParser
    ) : this(
        syncHttpClient = SynchronousHttpClient(socketFactory, httpResponseParser),
        scheduler = ThreadScheduler()
    )

    @Throws(Exception::class)
    fun sendRequest(request: HttpRequest): String {
        return syncHttpClient.request(request).body ?: ""
    }

    fun sendRequest(
        request: HttpRequest,
        callback: NetworkResponseCallback?,
        retryStrategy: RetryStrategy = RetryStrategy.NO_RETRY,
    ) {
        scheduleRequest(request, retryStrategy, callback)
    }

    @Suppress("TooGenericExceptionCaught")
    private fun scheduleRequest(
        request: HttpRequest,
        retryStrategy: RetryStrategy,
        callback: NetworkResponseCallback?
    ) {
        resetRetryCount(request)

        scheduler.runOnBackground {
            try {
                val httpResponse = syncHttpClient.request(request)
                callback?.let {
                    scheduler.runOnMain { callback.onResult(httpResponse, null) }
                }
            } catch (e: Exception) {
                when (retryStrategy) {
                    RetryStrategy.NO_RETRY -> notifyErrorOnMainThread(callback, e)
                    RetryStrategy.RETRY_MAX_3_TIMES -> retryGet(request, retryStrategy, callback)
                }
            }
        }
    }

    private fun retryGet(
        request: HttpRequest,
        retryStrategy: RetryStrategy,
        callback: NetworkResponseCallback?
    ) {
        var url: URL? = null
        try {
            url = request.url
        } catch (ignore: MalformedURLException) {
        } catch (ignore: URISyntaxException) {
        }

        if (url != null) {
            val retryCount = getNumRetriesSoFar(url)
            val shouldRetry = ((retryCount + 1) < MAX_RETRY_ATTEMPTS)
            if (shouldRetry) {
                scheduleRequest(request, retryStrategy, callback)
                retryCountMap[url] = retryCount + 1
            } else {
                val message = "Retry limit has been exceeded. Try again later."
                val retryLimitException = HttpClientException(message)
                notifyErrorOnMainThread(callback, retryLimitException)
            }
        }
    }

    private fun getNumRetriesSoFar(url: URL): Int {
        val retryCount = retryCountMap[url] ?: return 0
        return retryCount
    }

    private fun resetRetryCount(request: HttpRequest) {
        try {
            request.url?.let { retryCountMap.remove(it) }
        } catch (ignore: MalformedURLException) {
        } catch (ignore: URISyntaxException) {
        }
    }

    private fun notifyErrorOnMainThread(callback: NetworkResponseCallback?, e: Exception) {
        if (callback != null) {
            scheduler.runOnMain { callback.onResult(null, e) }
        }
    }

    companion object {
        private const val MAX_RETRY_ATTEMPTS: Int = 3
    }
}
