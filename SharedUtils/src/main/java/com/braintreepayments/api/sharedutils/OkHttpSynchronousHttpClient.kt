package com.braintreepayments.api.sharedutils

import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Synchronous HTTP client using OkHttp for network requests.
 *
 * This client is intended for internal use and provides synchronous HTTP operations
 * with certificate pinning support. It wraps OkHttp and exposes a blocking request method.
 *
 * @property socketFactory TLSSocketFactory used for SSL connections
 * @property okHttpClient OkHttpClient instance
 */
internal class OkHttpSynchronousHttpClient(
    private val socketFactory: TLSSocketFactory = TLSSocketFactory(),
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(THIRTY, TimeUnit.SECONDS)
        .readTimeout(THIRTY, TimeUnit.SECONDS)
        .sslSocketFactory(socketFactory, socketFactory.trustManager)
        .addInterceptor(GzipRequestInterceptor())
        .build(),
) {

    /**
     * Executes a synchronous HTTP request using OkHttp.
     *
     * This method builds an OkHttp [Request] from the provided [OkHttpRequest],
     * executes it synchronously, and returns the response as an [HttpResponse].
     *
     * @param okHttpRequest The request data to execute.
     * @return The HTTP response containing the body and timing information.
     * @throws Exception if the request fails or the response is unsuccessful.
     */
    @Throws(Exception::class)
    fun executeRequest(okHttpRequest: OkHttpRequest): HttpResponse {
        val startTime = System.currentTimeMillis()
        val request = okHttpRequest.toRequest()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            val endTime = System.currentTimeMillis()
            return HttpResponse(
                body = response.body?.string(),
                timing = HttpResponseTiming(startTime, endTime)
            )
        }
    }

    /**
     * Converts an [OkHttpRequest] to an OkHttp [Request].
     *
     * This function builds the OkHttp [Request] by setting the URL, HTTP method,
     * headers, and request body as specified in the [OkHttpRequest].
     *
     * @receiver The [OkHttpRequest] to convert.
     * @return The constructed OkHttp [Request] object.
     */
    private fun OkHttpRequest.toRequest(): Request {
        val headersBuilder = Headers.Builder()
        for (header in headers) {
            headersBuilder.add(header.key, header.value)
        }

        val (method, requestBody) = when (method) {
            is Method.Get -> method.stringValue to null
            is Method.Post -> method.stringValue to method.body.toRequestBody("application/json".toMediaTypeOrNull())
        }

        return Request.Builder()
            .url(url)
            .method(method, requestBody)
            .headers(headersBuilder.build())
            .build()
    }

    companion object {
        private const val THIRTY = 30L
    }
}
