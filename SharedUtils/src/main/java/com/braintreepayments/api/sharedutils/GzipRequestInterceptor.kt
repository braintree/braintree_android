package com.braintreepayments.api.sharedutils

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import okio.GzipSource
import okio.buffer

/**
 * An OkHttp interceptor that automatically decompresses gzip-encoded HTTP responses.
 *
 * This interceptor checks for responses with "Content-Encoding: gzip" header and
 * transparently decompresses the response body. After decompression, it removes
 * the Content-Encoding and Content-Length headers since they no longer apply
 * to the decompressed content.
 *
 * The interceptor handles case-insensitive matching for the gzip encoding
 * (e.g., "gzip", "GZIP", "Gzip" are all supported).
 */
internal class GzipRequestInterceptor : Interceptor {

    /**
     * Intercepts HTTP responses and decompresses gzip-encoded content.
     *
     * This method examines the Content-Encoding header of the response and:
     * - If the response is gzip-encoded, decompresses the body and removes
     *   Content-Encoding and Content-Length headers
     * - If the response is not gzip-encoded, returns the original response unchanged
     *
     * @param chain The interceptor chain to proceed with the request
     * @return The response with decompressed body if it was gzip-encoded,
     *         otherwise the original response
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())

        return if (originalResponse.header("Content-Encoding").equals("gzip", ignoreCase = true)) {
            val headers = originalResponse.headers.newBuilder()
                .removeAll("Content-Encoding")
                .removeAll("Content-Length")
                .build()

            val body = originalResponse.body?.let { body ->
                GzipSource(body.source()).buffer().asResponseBody(body.contentType(), -1)
            }

            originalResponse.newBuilder()
                .headers(headers)
                .body(body)
                .build()
        } else {
            originalResponse
        }
    }
}
