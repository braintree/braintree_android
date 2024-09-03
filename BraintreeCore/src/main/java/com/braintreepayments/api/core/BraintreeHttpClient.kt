package com.braintreepayments.api.core

import android.net.Uri
import com.braintreepayments.api.sharedutils.HttpClient
import com.braintreepayments.api.sharedutils.HttpMethod
import com.braintreepayments.api.sharedutils.HttpRequest
import com.braintreepayments.api.sharedutils.HttpResponse
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
     * Make an asynchronous Braintree authenticated HTTP request.
     *
     * @param request the http request.
     * @param callback See [NetworkResponseCallback].
     */
    @Suppress("TooGenericExceptionCaught")
    fun sendRequest(
        request: InternalHttpRequest,
        configuration: Configuration? = null,
        authorization: Authorization? = null,
        callback: NetworkResponseCallback?
    ) {
        try {
            val httpRequest = buildHttpRequest(request, configuration, authorization)
            httpClient.sendRequest(httpRequest, callback)
        } catch (error: Exception) {
            // forward errors
            callback?.onResult(null, error)
        }
    }

    /**
     * Make a synchronous Braintree authenticated HTTP request. This method is useful when
     * mutli-threading logic is managed by another entity e.g. WorkManager, ExecutorService.
     *
     * @param request the braintree http request.
     */
    @Throws(Exception::class)
    fun sendRequestSync(
        request: InternalHttpRequest,
        configuration: Configuration?,
        authorization: Authorization?
    ): HttpResponse {
        val httpRequest = buildHttpRequest(request, configuration, authorization)
        return httpClient.sendRequest(httpRequest)
    }

    @Suppress("CyclomaticComplexMethod")
    private fun buildHttpRequest(
        request: InternalHttpRequest,
        configuration: Configuration?,
        authorization: Authorization?,
    ): HttpRequest = request.run {
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

        val targetPath = if (method == HttpMethod.GET && authorization is ClientToken) {
            Uri.parse(path)
                .buildUpon()
                .appendQueryParameter(AUTHORIZATION_FINGERPRINT_KEY, authorization.bearer)
                .toString()
        } else {
            path
        }

        val requestData = if (method == HttpMethod.POST) {
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

        if (method == HttpMethod.POST) {
            authorization?.bearer?.let { token -> result.addHeader("Authorization", "Bearer $token") }
        }
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
