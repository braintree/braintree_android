package com.braintreepayments.api.core

import com.braintreepayments.api.sharedutils.HttpClient
import com.braintreepayments.api.sharedutils.Method
import com.braintreepayments.api.sharedutils.NetworkResponseCallback
import com.braintreepayments.api.sharedutils.OkHttpRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

internal class BraintreeGraphQLClient(
    private val httpClient: HttpClient = HttpClient()
) {

    fun post(
        data: String,
        configuration: Configuration,
        authorization: Authorization,
        callback: NetworkResponseCallback
    ) {
        if (authorization is InvalidAuthorization) {
            val message = authorization.errorMessage
            callback.onResult(NetworkResponseCallback.Result.Failure(BraintreeException(message)))
            return
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

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = httpClient.sendRequest(request)
                callback.onResult(NetworkResponseCallback.Result.Success(response))
            } catch (e: Exception) {
                callback.onResult(NetworkResponseCallback.Result.Failure(e))
            }
        }
    }
}
