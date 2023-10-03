package com.braintreepayments.api

import java.util.Locale

internal class BraintreeGraphQLClient(
    private val httpClient: HttpClient = createDefaultHttpClient()
) {

    fun post(
        path: String?,
        data: String?,
        configuration: Configuration,
        authorization: Authorization,
        callback: HttpResponseCallback
    ) {
        if (authorization is InvalidAuthorization) {
            val message = authorization.errorMessage
            callback.onResult(null, BraintreeException(message))
            return
        }
        val request = HttpRequest()
            .method("POST")
            .path(path)
            .data(data)
            .baseUrl(configuration.graphQLUrl)
            .addHeader("User-Agent", "braintree/android/" + BuildConfig.VERSION_NAME)
            .addHeader("Authorization",
                String.format(Locale.US, "Bearer %s", authorization.bearer))
            .addHeader("Braintree-Version", GraphQLConstants.Headers.API_VERSION)
        httpClient.sendRequest(request, callback)
    }

    fun post(
        data: String?,
        configuration: Configuration,
        authorization: Authorization,
        callback: HttpResponseCallback
    ) {
        if (authorization is InvalidAuthorization) {
            val message = authorization.errorMessage
            callback.onResult(null, BraintreeException(message))
            return
        }
        val request = HttpRequest()
            .method("POST")
            .path("")
            .data(data)
            .baseUrl(configuration.graphQLUrl)
            .addHeader("User-Agent", "braintree/android/" + BuildConfig.VERSION_NAME)
            .addHeader("Authorization",
                String.format(Locale.US, "Bearer %s", authorization.bearer))
            .addHeader("Braintree-Version", GraphQLConstants.Headers.API_VERSION)
        httpClient.sendRequest(request, callback)
    }

    @Throws(Exception::class)
    fun post(
        path: String?,
        data: String?,
        configuration: Configuration,
        authorization: Authorization
    ): String {
        if (authorization is InvalidAuthorization) {
            val message = authorization.errorMessage
            throw BraintreeException(message)
        }
        val request = HttpRequest()
            .method("POST")
            .path(path)
            .data(data)
            .baseUrl(configuration.graphQLUrl)
            .addHeader("User-Agent", "braintree/android/" + BuildConfig.VERSION_NAME)
            .addHeader("Authorization",
                String.format(Locale.US, "Bearer %s", authorization.bearer))
            .addHeader("Braintree-Version", GraphQLConstants.Headers.API_VERSION)
        return httpClient.sendRequest(request)
    }

    companion object {

        private fun createDefaultHttpClient(): HttpClient {
            val socketFactory = TLSSocketFactory(TLSCertificatePinning.certInputStream)
            return HttpClient(socketFactory, BraintreeGraphQLResponseParser())
        }
    }
}
