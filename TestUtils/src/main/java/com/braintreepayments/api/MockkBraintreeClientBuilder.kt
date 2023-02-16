package com.braintreepayments.api

import io.mockk.every
import io.mockk.mockk

class MockkBraintreeClientBuilder {

    private var sessionId: String = "session-id-from-mockk-braintree-client-builder"

    private var sendGraphQLPostSuccess: String? = null
    private var sendGraphQLPOSTError: ErrorWithResponse? = null

    private var configurationSuccess: Configuration? = null
    private var authorizationSuccess: Authorization? = null

    fun configurationSuccess(configurationSuccess: Configuration): MockkBraintreeClientBuilder {
        this.configurationSuccess = configurationSuccess
        return this
    }

    fun sessionId(sessionId: String): MockkBraintreeClientBuilder {
        this.sessionId = sessionId
        return this
    }

    fun authorizationSuccess(authorizationSuccess: Authorization): MockkBraintreeClientBuilder {
        this.authorizationSuccess = authorizationSuccess
        return this;
    }

    fun build(): BraintreeClient {
        val braintreeClient = mockk<BraintreeClient>(relaxed = true)
        every { braintreeClient.sessionId } returns sessionId

        every { braintreeClient.getConfiguration(any()) } answers { call ->
            val callback = call.invocation.args[0] as ConfigurationCallback
            configurationSuccess?.let { callback.onResult(it, null) }
        }

        every { braintreeClient.getAuthorization(any()) } answers { call ->
            val callback = call.invocation.args[0] as AuthorizationCallback
            authorizationSuccess?.let { callback.onAuthorizationResult(it, null) }
        }

        every { braintreeClient.sendGraphQLPOST(any(), any()) } answers { call ->
            val callback = call.invocation.args[1] as HttpResponseCallback
            sendGraphQLPostSuccess?.let { callback.onResult(it, null) }
                ?: sendGraphQLPOSTError?.let { callback.onResult(null, it) }
        }

        return braintreeClient
    }

    fun sendGraphQLPOSTSuccessfulResponse(sendGraphQLPostSuccess: String): MockkBraintreeClientBuilder {
        this.sendGraphQLPostSuccess = sendGraphQLPostSuccess
        return this
    }

    fun sendGraphQLPOSTErrorResponse(sendGraphQLPOSTError: ErrorWithResponse?): MockkBraintreeClientBuilder {
        this.sendGraphQLPOSTError = sendGraphQLPOSTError
        return this
    }
}