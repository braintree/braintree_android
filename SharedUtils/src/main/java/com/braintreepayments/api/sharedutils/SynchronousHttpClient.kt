package com.braintreepayments.api.sharedutils

import androidx.annotation.RestrictTo
import java.net.HttpURLConnection
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

/**
 * This class performs an http request on the calling thread. The external caller is
 * responsible for thread scheduling to ensure that this is not called on the main thread.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class SynchronousHttpClient(
    private val socketFactory: SSLSocketFactory,
    private val parser: HttpResponseParser
) {

    @Throws(Exception::class)
    fun request(httpRequest: HttpRequest): HttpResponse {
        requireNotNull(httpRequest.path) { "Path cannot be null" }

        val url = httpRequest.url
        val startTime = System.currentTimeMillis()

        val connection = url.openConnection() as HttpURLConnection
        if (connection is HttpsURLConnection) {
            connection.sslSocketFactory = socketFactory
        }

        val requestMethod = httpRequest.method
        connection.requestMethod = requestMethod

        connection.readTimeout = httpRequest.readTimeout
        connection.connectTimeout = httpRequest.connectTimeout

        // apply request headers
        val headers = httpRequest.headers
        for ((key, value) in headers) {
            connection.setRequestProperty(key, value)
        }

        if (requestMethod != null && requestMethod == "POST") {
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val outputStream = connection.outputStream
            outputStream.write(httpRequest.data)
            outputStream.flush()
            outputStream.close()

            httpRequest.dispose()
        }

        try {
            val responseCode = connection.responseCode
            val endTime = System.currentTimeMillis()

            return HttpResponse(
                body = parser.parse(responseCode, connection),
                timing = HttpResponseTiming(startTime, endTime)
            )
        } finally {
            connection.disconnect()
        }
    }
}
