package com.braintreepayments.api.sharedutils

import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okio.Buffer
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
        .build(),
) {

    // OkHttp follows redirects between https and http by default. Byte downloads must never make a cleartext hop, so
    // they use a client that doesn't follow a redirect that changes the scheme; it shares the connection pool and TLS
    // setup of [okHttpClient].
    private val bytesOkHttpClient: OkHttpClient by lazy {
        okHttpClient.newBuilder().followSslRedirects(false).build()
    }

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
            val responseBody = response.body?.string()
            if (!response.isSuccessful) throw IOException("Unexpected code $response with body $responseBody")
            val endTime = System.currentTimeMillis()
            return HttpResponse(
                body = responseBody,
                timing = HttpResponseTiming(startTime, endTime)
            )
        }
    }

    /**
     * Executes a synchronous HTTP request using OkHttp and returns the raw response body.
     *
     * Use this for binary payloads such as images, which [executeRequest] would corrupt by decoding the body as a
     * [String].
     *
     * @param okHttpRequest The request data to execute.
     * @param maxBodyBytes Limit on the response body size, [DEFAULT_MAX_BODY_BYTES] by default. Rejected as soon as the
     * declared `Content-Length` exceeds it, or, if undeclared (e.g. chunked transfer), as soon as actual bytes read
     * exceed it — the body is never buffered past [maxBodyBytes] + 1 bytes.
     * @return The response body bytes.
     * @throws IOException if the request fails, the response is unsuccessful or has no body (including a redirect
     * between https and http, which is never followed), the body size exceeds [maxBodyBytes], or the whole call, body
     * included, takes longer than [BYTES_CALL_TIMEOUT_SECONDS].
     * @throws IllegalArgumentException if the request URL is not a valid http or https URL.
     */
    @Throws(IOException::class)
    fun executeRequestForBytes(okHttpRequest: OkHttpRequest, maxBodyBytes: Long = DEFAULT_MAX_BODY_BYTES): ByteArray {
        val request = okHttpRequest.toRequest()

        // The connect and read timeouts only bound each step, so a server that trickles bytes could hold the call open
        // for as long as it keeps sending; this bounds the whole call.
        val call = bytesOkHttpClient.newCall(request)
        call.timeout().timeout(BYTES_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        call.execute().use { response ->
            val responseBody = response.body?.takeIf { response.isSuccessful }
                ?: throw IOException("Unexpected code ${response.code} for ${request.url}")
            return responseBody.readBoundedBytes(maxBodyBytes, request.url.toString())
        }
    }

    // Bounded regardless of the declared Content-Length, so a response that omits or understates it (e.g. chunked
    // transfer) still can't be buffered unbounded in memory.
    private fun ResponseBody.readBoundedBytes(maxBodyBytes: Long, url: String): ByteArray {
        if (contentLength() > maxBodyBytes) throwBodyTooLarge(maxBodyBytes, url)

        // Saturated so a limit of Long.MAX_VALUE can't overflow to a negative read limit.
        val limit = if (maxBodyBytes == Long.MAX_VALUE) maxBodyBytes else maxBodyBytes + 1
        val buffer = Buffer()
        val source = source()
        while (buffer.size < limit && source.read(buffer, limit - buffer.size) != -1L) {
            // keep reading until EOF or the limit is reached
        }
        if (buffer.size > maxBodyBytes) throwBodyTooLarge(maxBodyBytes, url)
        return buffer.readByteArray()
    }

    private fun throwBodyTooLarge(maxBodyBytes: Long, url: String): Nothing =
        throw IOException("Response body exceeds $maxBodyBytes bytes for $url")

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

        /** How long [executeRequestForBytes] may take in total, from connecting to reading the last byte. */
        private const val BYTES_CALL_TIMEOUT_SECONDS = THIRTY

        /** The largest response body [executeRequestForBytes] reads unless the caller asks for a different limit. */
        const val DEFAULT_MAX_BODY_BYTES = 5L * 1024L * 1024L
    }
}
