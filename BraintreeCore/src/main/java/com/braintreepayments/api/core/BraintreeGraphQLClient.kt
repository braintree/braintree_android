package com.braintreepayments.api.core

import com.braintreepayments.api.sharedutils.HttpClient
import com.braintreepayments.api.sharedutils.HttpRequest
import com.braintreepayments.api.sharedutils.NetworkResponseCallback
import com.braintreepayments.api.sharedutils.TLSSocketFactory
import java.util.Locale
import javax.net.ssl.SSLException

internal class BraintreeGraphQLClient(
    private val httpClient: HttpClient = createDefaultHttpClient()
) {

    fun post(
        data: String,
        configuration: Configuration,
        authorization: Authorization,
        callback: NetworkResponseCallback
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

    companion object {

        @Throws(SSLException::class)
        private fun createDefaultHttpClient(): HttpClient {
            val socketFactory = TLSSocketFactory(TLSCertificatePinning.createCertificateInputStream())
            return HttpClient(socketFactory, BraintreeGraphQLResponseParser())
        }
    }
}
