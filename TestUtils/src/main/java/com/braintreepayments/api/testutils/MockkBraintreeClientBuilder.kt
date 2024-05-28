package com.braintreepayments.api.testutils

import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.ConfigurationCallback
import com.braintreepayments.api.core.ErrorWithResponse
import com.braintreepayments.api.sharedutils.HttpResponseCallback
import io.mockk.every
import io.mockk.mockk

class MockkBraintreeClientBuilder {

    private var sessionId: String = "session-id-from-mockk-braintree-client-builder"

    private var sendGraphQLPostSuccess: String? = null
    private var sendGraphQLPOSTError: ErrorWithResponse? = null

    private var configurationSuccess: Configuration? = null
    private var configurationException: Exception? = null

    fun configurationSuccess(configurationSuccess: Configuration): MockkBraintreeClientBuilder {
        this.configurationSuccess = configurationSuccess
        return this
    }

    fun configurationError(error: Exception): MockkBraintreeClientBuilder {
        this.configurationException = error
        return this
    }

    fun sessionId(sessionId: String): MockkBraintreeClientBuilder {
        this.sessionId = sessionId
        return this
    }

    fun build(): BraintreeClient {
        val braintreeClient = mockk<BraintreeClient>(relaxed = true)
        every { braintreeClient.sessionId } returns sessionId

        every { braintreeClient.getConfiguration(any()) } answers { call ->
            val callback = call.invocation.args[0] as ConfigurationCallback
            callback.onResult(configurationSuccess, configurationException)
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
