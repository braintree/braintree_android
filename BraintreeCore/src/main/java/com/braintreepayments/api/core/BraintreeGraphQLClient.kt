package com.braintreepayments.api.core

import com.braintreepayments.api.sharedutils.HttpClient
import com.braintreepayments.api.sharedutils.HttpResponse
import com.braintreepayments.api.sharedutils.Method
import com.braintreepayments.api.sharedutils.OkHttpRequest
import java.util.Locale

internal class BraintreeGraphQLClient(
    private val httpClient: HttpClient = HttpClient(),
) {

    /**
     * @throws BraintreeException if authorization is invalid
     * @throws Exception if the network request fails
     */
    suspend fun post(
        data: String,
        configuration: Configuration,
        authorization: Authorization,
    ): HttpResponse {
        if (authorization is InvalidAuthorization) {
            val message = authorization.errorMessage
            throw BraintreeException(message)
        }

        val request = OkHttpRequest(
            method = Method.Post(data),
            url = configuration.graphQLUrl,
            headers = mapOf(
                "User-Agent" to "braintree/android/" + BuildConfig.VERSION_NAME,
                "Authorization" to String.format(Locale.US, "Bearer %s", authorization.bearer),
                "Braintree-Version" to GraphQLConstants.Headers.API_VERSION
            )
        )

        return httpClient.sendRequest(request)
    }
}
