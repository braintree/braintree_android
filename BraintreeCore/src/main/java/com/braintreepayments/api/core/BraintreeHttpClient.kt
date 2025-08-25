package com.braintreepayments.api.core

import androidx.core.net.toUri
import com.braintreepayments.api.sharedutils.HttpClient
import com.braintreepayments.api.sharedutils.Method
import com.braintreepayments.api.sharedutils.NetworkResponseCallback
import com.braintreepayments.api.sharedutils.OkHttpRequest
import org.json.JSONException
import org.json.JSONObject

/**
 * Network request class that handles Braintree request specifics and threading.
 */
internal class BraintreeHttpClient(
    private val httpClient: HttpClient = HttpClient()
) {

    /**
     * Make a HTTP GET request to Braintree using the base url, path and authorization provided.
     * If the path is a full url, it will be used instead of the previously provided url.
     */
    fun get(
        path: String,
        configuration: Configuration?,
        authorization: Authorization?,
        callback: NetworkResponseCallback
    ) {
        if (!validateAuthorization(authorization, callback)) return

        val url = try {
            val urlWithBase = assembleUrl(path, configuration)
            if (authorization is ClientToken) {
                urlWithBase.toUri().buildUpon()
                    .appendQueryParameter(AUTHORIZATION_FINGERPRINT_KEY, authorization.bearer)
                    .toString()
            } else {
                urlWithBase
            }
        } catch (e: BraintreeException) {
            callback.onResult(null, e)
            return
        }

        val request = OkHttpRequest(
            method = Method.Get,
            url = url,
            headers = assembleHeaders(authorization)
        )

        httpClient.sendRequest(request, callback)
    }

    /**
     * Make a HTTP POST request to Braintree.
     * If the path is a full url, it will be used instead of the previously provided url.
     */
    fun post(
        path: String,
        data: String,
        configuration: Configuration?,
        authorization: Authorization?,
        additionalHeaders: Map<String, String> = emptyMap(),
        callback: NetworkResponseCallback?
    ) {
        if (!validateAuthorization(authorization, callback)) return

        val requestBody = if (authorization is ClientToken) {
            try {
                JSONObject(data)
                    .put(AUTHORIZATION_FINGERPRINT_KEY, authorization.authorizationFingerprint)
                    .toString()
            } catch (e: JSONException) {
                callback?.onResult(null, e)
                return
            }
        } else {
            data
        }

        val url = try {
            assembleUrl(path, configuration)
        } catch (e: BraintreeException) {
            callback?.onResult(null, e)
            return
        }

        val request = OkHttpRequest(
            method = Method.Post(requestBody),
            url = url,
            headers = assembleHeaders(authorization, additionalHeaders)
        )

        httpClient.sendRequest(request, callback)
    }

    private fun validateAuthorization(
        authorization: Authorization?,
        callback: NetworkResponseCallback?
    ): Boolean {
        if (authorization is InvalidAuthorization) {
            callback?.onResult(null, BraintreeException(authorization.errorMessage))
            return false
        }
        return true
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
