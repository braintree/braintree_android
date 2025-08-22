package com.braintreepayments.api.sharedutils

import androidx.annotation.RestrictTo
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_ACCEPTED
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_CREATED
import java.net.HttpURLConnection.HTTP_FORBIDDEN
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_OK
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import java.net.HttpURLConnection.HTTP_UNAVAILABLE
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class BaseHttpResponseParser : HttpResponseParser {

    companion object {
        private const val HTTP_UNPROCESSABLE_ENTITY = 422
        private const val HTTP_UPGRADE_REQUIRED = 426
        private const val HTTP_TOO_MANY_REQUESTS = 429

        private const val BYTE_ARRAY_SIZE = 1024
    }

    @Throws(Exception::class)
    override fun parse(responseCode: Int, connection: HttpURLConnection): String {
        val responseBody = parseBody(responseCode, connection) ?: "empty responseBody"
        return when (responseCode) {
            HTTP_OK, HTTP_CREATED, HTTP_ACCEPTED -> responseBody
            HTTP_BAD_REQUEST, HTTP_UNPROCESSABLE_ENTITY -> throw UnprocessableEntityException(responseBody)
            HTTP_UNAUTHORIZED -> throw AuthenticationException(responseBody)
            HTTP_FORBIDDEN -> throw AuthorizationException(responseBody)
            HTTP_UPGRADE_REQUIRED -> throw UpgradeRequiredException(responseBody)
            HTTP_TOO_MANY_REQUESTS -> {
                throw RateLimitException("You are being rate-limited. Please try again in a few minutes.")
            }
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
    private fun readStream(inputStream: InputStream?, gzip: Boolean): String? {
        if (inputStream == null) {
            return null
        }

        val out = ByteArrayOutputStream()
        var stream = inputStream
        try {
            if (gzip) {
                stream = GZIPInputStream(inputStream)
            }

            val buffer = ByteArray(BYTE_ARRAY_SIZE)
            var count: Int
            while (stream.read(buffer).also { count = it } != -1) {
                out.write(buffer, 0, count)
            }
            return String(out.toByteArray(), StandardCharsets.UTF_8)
        } finally {
            try {
                inputStream.close()
            } catch (_: IOException) {
            }
        }
    }
}
