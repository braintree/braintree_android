package com.braintreepayments.api.core

import androidx.core.net.toUri
import com.braintreepayments.api.sharedutils.HttpClient
import com.braintreepayments.api.sharedutils.HttpResponse
import com.braintreepayments.api.sharedutils.Method
import com.braintreepayments.api.sharedutils.OkHttpRequest
import org.json.JSONObject

/**
 * Network request class that handles Braintree request specifics and threading.
 */
internal class BraintreeHttpClient(
    private val httpClient: HttpClient = HttpClient(),
) {

    /**
     * Make a HTTP GET request to Braintree using the base url, path and authorization provided.
     * If the path is a full url, it will be used instead of the previously provided url.
     */
    suspend fun get(
        path: String,
        configuration: Configuration?,
        authorization: Authorization?,
    ): HttpResponse {

        validateAuthorization(authorization)

        val url =
            if (authorization is ClientToken) {
                val urlWithBase = assembleUrl(path, configuration)
                urlWithBase.toUri().buildUpon()
                    .appendQueryParameter(AUTHORIZATION_FINGERPRINT_KEY, authorization.bearer)
                    .toString()
            } else {
                assembleUrl(path, configuration)
            }

        val request = OkHttpRequest(
            method = Method.Get,
            url = url,
            headers = assembleHeaders(authorization)
        )

        return httpClient.sendRequest(request)
    }

    /**
     * Make a HTTP POST request to Braintree.
     * If the path is a full url, it will be used instead of the previously provided url.
     */
    suspend fun post(
        path: String,
        data: String,
        configuration: Configuration?,
        authorization: Authorization?,
        additionalHeaders: Map<String, String> = emptyMap(),
    ): HttpResponse {

        validateAuthorization(authorization)

        val requestBody =
            if (authorization is ClientToken) {
                JSONObject(data)
                    .put(AUTHORIZATION_FINGERPRINT_KEY, authorization.authorizationFingerprint)
                    .toString()
            } else {
                data
            }

        val url = assembleUrl(path, configuration)

        val request = OkHttpRequest(
            method = Method.Post(requestBody),
            url = url,
            headers = assembleHeaders(authorization, additionalHeaders)
        )

        return httpClient.sendRequest(request)
    }

    private fun validateAuthorization(
        authorization: Authorization?,
    ) {
        if (authorization is InvalidAuthorization) {
            val message = authorization.errorMessage
            throw BraintreeException(message)
        }
    }

    @Throws(BraintreeException::class)
    private fun assembleUrl(path: String, configuration: Configuration?): String {
        return if (path.startsWith("http")) {
            path
        } else {
            configuration?.clientApiUrl?.plus(path)
                ?: throw BraintreeException("Braintree HTTP request without configuration cannot have a relative path.")
        }
    }

    private fun assembleHeaders(
        authorization: Authorization?,
        additionalHeaders: Map<String, String> = emptyMap()
    ): Map<String, String> {
        val headers = mutableMapOf(
            USER_AGENT_HEADER to "braintree/android/" + BuildConfig.VERSION_NAME
        )
        when (authorization) {
            is TokenizationKey -> headers["Client-Key"] = authorization.bearer
            is ClientToken -> headers["Authorization"] = "Bearer ${authorization.bearer}"
        }
        headers.putAll(additionalHeaders)
        return headers
    }

    companion object {
        private const val AUTHORIZATION_FINGERPRINT_KEY = "authorizationFingerprint"
        private const val USER_AGENT_HEADER = "User-Agent"
    }
}
