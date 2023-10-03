package com.braintreepayments.api

import android.net.Uri
import com.braintreepayments.api.HttpClient.RetryStrategy
import org.json.JSONException
import org.json.JSONObject

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
     * @param callback [HttpResponseCallback]
     */
    operator fun get(
        path: String,
        configuration: Configuration?,
        authorization: Authorization?,
        callback: HttpResponseCallback
    ) = get(path, configuration, authorization, HttpClient.NO_RETRY, callback)

    /**
     * Make a HTTP GET request to Braintree using the base url, path and authorization provided.
     * If the path is a full url, it will be used instead of the previously provided url.
     * @param path The path or url to request from the server via GET
     * @param configuration configuration for the Braintree Android SDK.
     * @param authorization
     * @param retryStrategy retry strategy
     * @param callback [HttpResponseCallback]
     */
    operator fun get(
        path: String,
        configuration: Configuration?,
        authorization: Authorization?,
        @RetryStrategy retryStrategy: Int,
        callback: HttpResponseCallback
    ) {
        if (authorization is InvalidAuthorization) {
            val message = authorization.errorMessage
            callback.onResult(null, BraintreeException(message))
            return
        }
        val isRelativeURL = !path.startsWith("http")
        if (configuration == null && isRelativeURL) {
            val message =
                "Braintree HTTP GET request without configuration cannot have a relative path."
            val relativeURLNotAllowedError = BraintreeException(message)
            callback.onResult(null, relativeURLNotAllowedError)
            return
        }
        val targetPath = if (authorization is ClientToken) {
            Uri.parse(path).buildUpon()
                .appendQueryParameter(AUTHORIZATION_FINGERPRINT_KEY, authorization.bearer)
                .toString()
        } else {
            path
        }
        val request = HttpRequest().method("GET").path(targetPath)
            .addHeader(USER_AGENT_HEADER, "braintree/android/" + BuildConfig.VERSION_NAME)
        if (isRelativeURL && configuration != null) {
            request.baseUrl(configuration.clientApiUrl)
        }
        if (authorization is TokenizationKey) {
            request.addHeader(CLIENT_KEY_HEADER, authorization.bearer)
        }
        httpClient.sendRequest(request, retryStrategy, callback)
    }

    /**
     * Make a HTTP POST request to Braintree.
     * If the path is a full url, it will be used instead of the previously provided url.
     * @param path The path or url to request from the server via HTTP POST
     * @param data The body of the POST request
     * @param configuration configuration for the Braintree Android SDK.
     * @param authorization
     * @param callback [HttpResponseCallback]
     */
    fun post(
        path: String,
        data: String,
        configuration: Configuration?,
        authorization: Authorization?,
        callback: HttpResponseCallback
    ) {
        if (authorization is InvalidAuthorization) {
            val message = authorization.errorMessage
            callback.onResult(null, BraintreeException(message))
            return
        }
        val isRelativeURL = !path.startsWith("http")
        if (configuration == null && isRelativeURL) {
            val message =
                "Braintree HTTP GET request without configuration cannot have a relative path."
            val relativeURLNotAllowedError = BraintreeException(message)
            callback.onResult(null, relativeURLNotAllowedError)
            return
        }
        val requestData = if (authorization is ClientToken) {
            try {
                JSONObject(data).put(
                    AUTHORIZATION_FINGERPRINT_KEY,
                    authorization.authorizationFingerprint
                ).toString()
            } catch (e: JSONException) {
                callback.onResult(null, e)
                return
            }
        } else {
            data
        }
        val request = HttpRequest().method("POST").path(path).data(requestData)
            .addHeader(USER_AGENT_HEADER, "braintree/android/" + BuildConfig.VERSION_NAME)
        if (isRelativeURL && configuration != null) {
            request.baseUrl(configuration.clientApiUrl)
        }
        if (authorization is TokenizationKey) {
            request.addHeader(CLIENT_KEY_HEADER, authorization.bearer)
        }
        httpClient.sendRequest(request, callback)
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
        if (authorization is InvalidAuthorization) {
            val message = authorization.errorMessage
            throw BraintreeException(message)
        }
        val isRelativeURL = !path.startsWith("http")
        if (configuration == null && isRelativeURL) {
            val message =
                "Braintree HTTP GET request without configuration cannot have a relative path."
            throw BraintreeException(message)
        }
        val requestData = if (authorization is ClientToken) {
            JSONObject(data).put(
                AUTHORIZATION_FINGERPRINT_KEY,
                authorization.authorizationFingerprint
            ).toString()
        } else {
            data
        }
        val request = HttpRequest().method("POST").path(path).data(requestData)
            .addHeader(USER_AGENT_HEADER, "braintree/android/" + BuildConfig.VERSION_NAME)
        if (isRelativeURL && configuration != null) {
            request.baseUrl(configuration.clientApiUrl)
        }
        if (authorization is TokenizationKey) {
            request.addHeader(CLIENT_KEY_HEADER, authorization.bearer)
        }
        return httpClient.sendRequest(request)
    }

    companion object {
        private const val AUTHORIZATION_FINGERPRINT_KEY = "authorizationFingerprint"
        private const val USER_AGENT_HEADER = "User-Agent"
        private const val CLIENT_KEY_HEADER = "Client-Key"

        private fun createDefaultHttpClient(): HttpClient {
            val socketFactory = TLSSocketFactory(TLSCertificatePinning.certInputStream)
            return HttpClient(socketFactory, BraintreeHttpResponseParser())
        }
    }
}
