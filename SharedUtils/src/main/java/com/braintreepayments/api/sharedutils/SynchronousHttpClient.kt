package com.braintreepayments.api.sharedutils

import androidx.annotation.RestrictTo
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * This class performs an http request on the calling thread. The external caller is
 * responsible for thread scheduling to ensure that this is not called on the main thread.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class SynchronousHttpClient(
    private val socketFactory: SSLSocketFactory,
    private val parser: HttpResponseParser
) {

    // Cache the trust manager and OkHttpClient instance
    private val trustManager: X509TrustManager by lazy { createPlatformTrustManager() }
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .sslSocketFactory(socketFactory, trustManager)
            .readTimeout(DEFAULT_READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .eventListener(TimingEventListener())
            .connectTimeout(DEFAULT_CONNECT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .build()
    }

    companion object {
        private const val DEFAULT_READ_TIMEOUT_MS = 10000
        private const val DEFAULT_CONNECT_TIMEOUT_MS = 10000
    }

    fun createPlatformTrustManager(): X509TrustManager {
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(null as KeyStore?)
        val trustManagers = trustManagerFactory.trustManagers
        return trustManagers.first { it is X509TrustManager } as X509TrustManager
    }

    @Throws(Exception::class)
    fun request(httpRequest: HttpRequest): HttpResponse {
        requireNotNull(httpRequest.path) { "Path cannot be null" }

        val url = httpRequest.url
        val startTime = System.currentTimeMillis()

        val builder = Request.Builder()
            .url(url)

        // apply request headers
        val headers = httpRequest.headers
        for ((key, value) in headers) {
            builder.addHeader(key, value)
        }

        val requestMethod = httpRequest.method ?: "GET"
        if (requestMethod == "POST") {
            builder.addHeader("Content-Type", "application/json")
            val body = RequestBody.create(
                "application/json".toMediaTypeOrNull(),
                httpRequest.data ?: ByteArray(0)
            )
            builder.post(body)
            httpRequest.dispose()
        } else {
            builder.method(requestMethod, null)
        }

        val request = builder.build()

        val response: Response = okHttpClient.newCall(request).execute()
        val endTime = System.currentTimeMillis()

        return HttpResponse(
            body = parser.parse(response.code, mapOkHttpResponseToHttpURLConnection(response)),
            timing = HttpResponseTiming(startTime, endTime)
        )
    }

    /**
     * Maps an OkHttp Response to a HttpURLConnection-like interface for legacy parser compatibility.
     * Only implements the minimal subset needed by the parser.
     */
    private fun mapOkHttpResponseToHttpURLConnection(response: Response): java.net.HttpURLConnection {
        return object : java.net.HttpURLConnection(response.request.url.toUrl()) {
            override fun getResponseCode(): Int = response.code
            override fun getInputStream() = response.body?.byteStream()
                ?: throw java.io.IOException("No response body")

            override fun getHeaderField(name: String?): String? = response.header(name ?: "")
            override fun getHeaderFieldKey(n: Int): String? = response.headers.names().elementAtOrNull(n)
            override fun getHeaderField(n: Int): String? = response.headers.value(n)
            override fun disconnect() {}
            override fun usingProxy(): Boolean = false
            override fun connect() {}
        }
    }
}

// You may need to implement PlatformTrustManager or use a suitable X509TrustManager instance.
