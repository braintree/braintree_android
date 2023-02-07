package com.braintreepayments.api

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.HttpURLConnection.*
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

/**
 * Class that handles parsing http responses for [SynchronousHttpClient].
 */
internal class BaseHttpResponseParser : HttpResponseParser {

    /**
     * @param responseCode the response code returned when the http request was made.
     * @param connection the connection through which the http request was made.
     * @return the body of the http response.
     */
    @Throws(Exception::class)
    override fun parse(responseCode: Int, connection: HttpURLConnection): String {
        val responseBody = parseBody(responseCode, connection) ?: ""
        return when (responseCode) {
            HTTP_OK, HTTP_CREATED, HTTP_ACCEPTED -> responseBody
            HTTP_BAD_REQUEST, HTTP_UNPROCESSABLE_ENTITY ->
                throw UnprocessableEntityException(responseBody)
            HTTP_UNAUTHORIZED -> throw AuthenticationException(responseBody)
            HTTP_FORBIDDEN -> throw AuthorizationException(responseBody)
            HTTP_UPGRADE_REQUIRED -> throw UpgradeRequiredException(responseBody)
            HTTP_TOO_MANY_REQUESTS ->
                throw RateLimitException("You are being rate-limited. Please try again in a few minutes.")
            HTTP_INTERNAL_ERROR -> throw ServerException(responseBody)
            HTTP_UNAVAILABLE -> throw ServiceUnavailableException(responseBody)
            else -> throw UnexpectedException(responseBody)
        }
    }

    @Throws(IOException::class)
    private fun parseBody(responseCode: Int, connection: HttpURLConnection): String? {
        val gzip = "gzip" == connection.contentEncoding
        return when (responseCode) {
            HTTP_OK, HTTP_CREATED, HTTP_ACCEPTED -> readStream(connection.inputStream, gzip)
            HTTP_TOO_MANY_REQUESTS -> null
            HTTP_UNAUTHORIZED,
            HTTP_FORBIDDEN,
            HTTP_BAD_REQUEST,
            HTTP_UNPROCESSABLE_ENTITY,
            HTTP_UPGRADE_REQUIRED,
            HTTP_INTERNAL_ERROR,
            HTTP_UNAVAILABLE -> readStream(connection.errorStream, gzip)
            else -> readStream(connection.errorStream, gzip)
        }
    }

    @Throws(IOException::class)
    private fun readStream(inputStream: InputStream?, gzip: Boolean): String? =
        inputStream?.let {
            val input = if (gzip) GZIPInputStream(it) else it

            val out = ByteArrayOutputStream()
            return try {
                val buffer = ByteArray(1024)
                var count = input.read(buffer)
                while (count != EOF) {
                    out.write(buffer, 0, count)
                    count = input.read(buffer)
                }
                String(out.toByteArray(), StandardCharsets.UTF_8)
            } finally {
                try {
                    input.close()
                } catch (ignored: Exception) {
                }
            }
        }

    companion object {
        private const val EOF = -1

        private const val HTTP_UNPROCESSABLE_ENTITY = 422
        private const val HTTP_UPGRADE_REQUIRED = 426
        private const val HTTP_TOO_MANY_REQUESTS = 429
    }
}