package com.braintreepayments.api.core

import com.braintreepayments.api.sharedutils.HttpMethod
import com.braintreepayments.api.sharedutils.HttpRequest
import com.braintreepayments.api.sharedutils.HttpResponse
import com.braintreepayments.api.sharedutils.NetworkResponseCallback
import com.braintreepayments.api.sharedutils.Scheduler
import com.braintreepayments.api.sharedutils.SynchronousHttpClient
import com.braintreepayments.api.sharedutils.TLSSocketFactory
import com.braintreepayments.api.sharedutils.ThreadScheduler
import java.lang.ref.WeakReference
import java.util.Locale
import javax.net.ssl.SSLException

internal class BraintreeGraphQLClient(
    private val httpClient: SynchronousHttpClient = createDefaultHttpClient(),
    private val threadScheduler: Scheduler = ThreadScheduler(1)
) {

    fun post(
        path: String?,
        data: String?,
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
            .method(HttpMethod.POST)
            .path(path)
            .data(data)
            .baseUrl(configuration.graphQLUrl)
            .addHeader("User-Agent", "braintree/android/" + BuildConfig.VERSION_NAME)
            .addHeader(
                "Authorization",
                String.format(Locale.US, "Bearer %s", authorization.bearer)
            )
            .addHeader("Braintree-Version", GraphQLConstants.Headers.API_VERSION)
        sendRequestInBackground(request, callback)
    }

    fun post(
        data: String?,
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
            .method(HttpMethod.POST)
            .path("")
            .data(data)
            .baseUrl(configuration.graphQLUrl)
            .addHeader("User-Agent", "braintree/android/" + BuildConfig.VERSION_NAME)
            .addHeader(
                "Authorization",
                String.format(Locale.US, "Bearer %s", authorization.bearer)
            )
            .addHeader("Braintree-Version", GraphQLConstants.Headers.API_VERSION)
        sendRequestInBackground(request, callback)
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
            .method(HttpMethod.POST)
            .path(path)
            .data(data)
            .baseUrl(configuration.graphQLUrl)
            .addHeader("User-Agent", "braintree/android/" + BuildConfig.VERSION_NAME)
            .addHeader(
                "Authorization",
                String.format(Locale.US, "Bearer %s", authorization.bearer)
            )
            .addHeader("Braintree-Version", GraphQLConstants.Headers.API_VERSION)
        return httpClient.request(request).body ?: ""
    }

    private fun sendRequestInBackground(request: HttpRequest, callback: NetworkResponseCallback) {
        val cbRef = WeakReference(callback)
        threadScheduler.runOnBackground {
            var response: HttpResponse? = null
            var error: Exception? = null
            try {
                response = httpClient.request(request)
            } catch (e: Exception) {
                error = e
            }

            threadScheduler.runOnMain {
                val cb = cbRef.get()
                cb?.onResult(response, error)
            }
        }
    }

    companion object {

        @Throws(SSLException::class)
        private fun createDefaultHttpClient(): SynchronousHttpClient {
            val socketFactory =
                TLSSocketFactory(TLSCertificatePinning.createCertificateInputStream())
            return SynchronousHttpClient(socketFactory, BraintreeGraphQLResponseParser())
        }
    }
}
