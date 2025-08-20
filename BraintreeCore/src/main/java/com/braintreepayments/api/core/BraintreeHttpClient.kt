package com.braintreepayments.api.core

import android.net.Uri
import com.braintreepayments.api.sharedutils.HttpClient
import com.braintreepayments.api.sharedutils.HttpClient.RetryStrategy
import com.braintreepayments.api.sharedutils.HttpRequest
import com.braintreepayments.api.sharedutils.NetworkResponseCallback
import com.braintreepayments.api.sharedutils.TLSSocketFactory
import org.json.JSONException
import org.json.JSONObject

/**
 * Network request class that handles Braintree request specifics and threading.
 */
internal class BraintreeHttpClient(
    private val httpClient: HttpClient = HttpClient(
        socketFactory = TLSSocketFactory(),
        httpResponseParser = BraintreeHttpResponseParser()
    )
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
    ) = get(path, configuration, authorization, RetryStrategy.NO_RETRY, callback)

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
        retryStrategy: RetryStrategy,
        callback: NetworkResponseCallback
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
        httpClient.sendRequest(request, callback, retryStrategy)
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
        if (authorization is InvalidAuthorization) {
            val message = authorization.errorMessage
            callback?.onResult(null, BraintreeException(message))
            return
        }
        val isRelativeURL = !path.startsWith("http")
        if (configuration == null && isRelativeURL) {
            val message =
                "Braintree HTTP GET request without configuration cannot have a relative path."
            val relativeURLNotAllowedError = BraintreeException(message)
            callback?.onResult(null, relativeURLNotAllowedError)
            return
        }
        val requestData = if (authorization is ClientToken) {
            try {
                JSONObject(data).put(
                    AUTHORIZATION_FINGERPRINT_KEY,
                    authorization.authorizationFingerprint
                ).toString()
            } catch (e: JSONException) {
                callback?.onResult(null, e)
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
        authorization?.bearer?.let { token -> request.addHeader("Authorization", "Bearer $token") }
        additionalHeaders.forEach { (name, value) -> request.addHeader(name, value) }
        httpClient.sendRequest(request, callback)
    }

    companion object {
        private const val AUTHORIZATION_FINGERPRINT_KEY = "authorizationFingerprint"
        private const val USER_AGENT_HEADER = "User-Agent"
        private const val CLIENT_KEY_HEADER = "Client-Key"
    }
}
