package com.braintreepayments.api.core

import android.net.Uri
import com.braintreepayments.api.sharedutils.HttpClient
import com.braintreepayments.api.sharedutils.HttpClient.RetryStrategy
import com.braintreepayments.api.sharedutils.HttpRequest
import com.braintreepayments.api.sharedutils.NetworkResponseCallback
import com.braintreepayments.api.sharedutils.TLSSocketFactory
import org.json.JSONObject
import javax.net.ssl.SSLException

/**
 * Network request class that handles Braintree request specifics and threading.
 */
internal class BraintreeHttpClient(
    private val httpClient: HttpClient = createDefaultHttpClient()
) {

    /**
     * Make a HTTP GET request to Braintree using the base url, path and authorization provided.
     * If the path is a full url, it will be used instead of the previously provided url.
     * @param path The path or url to request from the server via GET
     * @param configuration configuration for the Braintree Android SDK.
     * @param authorization
     * @param callback [NetworkResponseCallback]
     */
    operator fun get(
        path: String,
        configuration: Configuration?,
        authorization: Authorization?,
        callback: NetworkResponseCallback
    ) = get(path, configuration, authorization, HttpClient.NO_RETRY, callback)

    /**
     * Make a HTTP GET request to Braintree using the base url, path and authorization provided.
     * If the path is a full url, it will be used instead of the previously provided url.
     * @param path The path or url to request from the server via GET
     * @param configuration configuration for the Braintree Android SDK.
     * @param authorization
     * @param retryStrategy retry strategy
     * @param callback [NetworkResponseCallback]
     */
    operator fun get(
        path: String,
        configuration: Configuration?,
        authorization: Authorization?,
        @RetryStrategy retryStrategy: Int,
        callback: NetworkResponseCallback
    ) {
        val request = BraintreeHttpRequest(
            method = "GET",
            path = path,
            configuration = configuration,
            authorization = authorization,
            retryStrategy = retryStrategy
        )
        sendRequest(request, callback)
    }

    /**
     * Make a HTTP POST request to Braintree.
     * If the path is a full url, it will be used instead of the previously provided url.
     * @param path The path or url to request from the server via HTTP POST
     * @param data The body of the POST request
     * @param configuration configuration for the Braintree Android SDK.
     * @param authorization
     * @param callback [NetworkResponseCallback]
     */
    @Suppress("CyclomaticComplexMethod")
    fun post(
        path: String,
        data: String,
        configuration: Configuration?,
        authorization: Authorization?,
        additionalHeaders: Map<String, String> = emptyMap(),
        callback: NetworkResponseCallback?
    ) {
        val request = BraintreeHttpRequest(
            method = "POST",
            path = path,
            data = data,
            configuration = configuration,
            authorization = authorization,
            additionalHeaders = additionalHeaders
        )
        sendRequest(request, callback)
    }

    /**
     * Makes a synchronous HTTP POST request to Braintree.
     *
     * @param path the path or url to request from the server via HTTP POST
     * @param data the body of the post request
     * @param configuration configuration for the Braintree Android SDK.
     * @param authorization
     * @return the HTTP response body
     */
    @Throws(Exception::class)
    fun post(
        path: String, data: String, configuration: Configuration?, authorization: Authorization?
    ): String {
        val request = BraintreeHttpRequest(
            method = "POST",
            path = path,
            data = data,
            configuration = configuration,
            authorization = authorization,
        )
        return sendRequestSync(request)
    }

    /**
     * Make an asynchronous Braintree authenticated HTTP request.
     *
     * @param request the braintree http request.
     * @param callback See [NetworkResponseCallback].
     */
    private fun sendRequest(request: BraintreeHttpRequest, callback: NetworkResponseCallback?) {
        try {
            val httpRequest = buildHttpRequest(request)
            httpClient.sendRequest(httpRequest, request.retryStrategy, callback)
        } catch (e: Exception) {
            // forward errors
            callback?.onResult(null, e)
        }
    }

    /**
     * Make a synchronous Braintree authenticated HTTP request. This method is useful when
     * mutli-threading logic is managed by another entity e.g. WorkManager, ExecutorService.
     *
     * @param request the braintree http request.
     */
    @Throws(Exception::class)
    fun sendRequestSync(request: BraintreeHttpRequest): String {
        val httpRequest = buildHttpRequest(request)
        return httpClient.sendRequest(httpRequest)
    }

    private fun buildHttpRequest(request: BraintreeHttpRequest): HttpRequest = request.run {
        if (authorization is InvalidAuthorization) {
            val message = authorization.errorMessage
            throw BraintreeException(message)
        }
        val isRelativeURL = !path.startsWith("http")
        if (configuration == null && isRelativeURL) {
            val message =
                "Braintree HTTP $method request without configuration cannot have a relative path."
            throw BraintreeException(message)
        }

        val targetPath = if (method == "GET" && authorization is ClientToken) {
            Uri.parse(path)
                .buildUpon()
                .appendQueryParameter(AUTHORIZATION_FINGERPRINT_KEY, authorization.bearer)
                .toString()
        } else {
            path
        }

        val requestData = if (method == "POST") {
            if (authorization is ClientToken) {
                JSONObject(data ?: "{}").put(
                    AUTHORIZATION_FINGERPRINT_KEY,
                    authorization.authorizationFingerprint
                ).toString()
            } else {
                data
            }
        } else {
            null
        }

        val result = HttpRequest()
            .method(method)
            .path(targetPath)
            .data(requestData ?: "")
            .addHeader(USER_AGENT_HEADER, "braintree/android/" + BuildConfig.VERSION_NAME)

        if (isRelativeURL && configuration != null) {
            result.baseUrl(configuration.clientApiUrl)
        }

        if (authorization is TokenizationKey) {
            result.addHeader(CLIENT_KEY_HEADER, authorization.bearer)
        }

        authorization?.bearer?.let { token -> result.addHeader("Authorization", "Bearer $token") }
        additionalHeaders.forEach { (name, value) -> result.addHeader(name, value) }
        return result
    }

    companion object {
        private const val AUTHORIZATION_FINGERPRINT_KEY = "authorizationFingerprint"
        private const val USER_AGENT_HEADER = "User-Agent"
        private const val CLIENT_KEY_HEADER = "Client-Key"

        @Throws(SSLException::class)
        private fun createDefaultHttpClient(): HttpClient {
            val socketFactory =
                TLSSocketFactory(TLSCertificatePinning.createCertificateInputStream())
            return HttpClient(socketFactory, BraintreeHttpResponseParser())
        }
    }
}
