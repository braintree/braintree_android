package com.braintreepayments.api.testutils

import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.ConfigurationCallback
import com.braintreepayments.api.sharedutils.HttpResponseCallback
import io.mockk.every
import io.mockk.mockk

class MockkBraintreeClientBuilder {

    private var sendGraphQLPostSuccess: String? = null
    private var sendGraphQLPOSTError: Exception? = null

    private var sendPOSTSuccess: String? = null
    private var sendPOSTError: Exception? = null

    private var configurationSuccess: Configuration? = null
    private var configurationException: Exception? = null
    private var authorizationSuccess: Authorization? = null

    private var launchesBrowserSwitchAsNewTask: Boolean = false
    private var returnUrlScheme: String = ""

    fun configurationSuccess(configurationSuccess: Configuration): MockkBraintreeClientBuilder {
        this.configurationSuccess = configurationSuccess
        return this
    }

    fun configurationError(error: Exception): MockkBraintreeClientBuilder {
        this.configurationException = error
        return this
    }

    fun authorizationSuccess(authorizationSuccess: Authorization): MockkBraintreeClientBuilder {
        this.authorizationSuccess = authorizationSuccess
        return this
    }

    fun launchesBrowserSwitchAsNewTask(launchesBrowserSwitchAsNewTask: Boolean): MockkBraintreeClientBuilder {
        this.launchesBrowserSwitchAsNewTask = launchesBrowserSwitchAsNewTask
        return this
    }

    fun returnUrlScheme(url: String): MockkBraintreeClientBuilder {
        this.returnUrlScheme = url
        return this
    }

    fun sendGraphQLPOSTSuccessfulResponse(sendGraphQLPostSuccess: String): MockkBraintreeClientBuilder {
        this.sendGraphQLPostSuccess = sendGraphQLPostSuccess
        return this
    }

    fun sendGraphQLPOSTErrorResponse(sendGraphQLPOSTError: Exception?): MockkBraintreeClientBuilder {
        this.sendGraphQLPOSTError = sendGraphQLPOSTError
        return this
    }

    fun sendPOSTSuccessfulResponse(sendPostSuccess: String): MockkBraintreeClientBuilder {
        this.sendPOSTSuccess = sendPostSuccess
        return this
    }

    fun sendPOSTErrorResponse(sendPOSTError: Exception?): MockkBraintreeClientBuilder {
        this.sendPOSTError = sendPOSTError
        return this
    }

    fun build(): BraintreeClient {
        val braintreeClient = mockk<BraintreeClient>(relaxed = true)

        every { braintreeClient.launchesBrowserSwitchAsNewTask() } returns launchesBrowserSwitchAsNewTask

        every { braintreeClient.getConfiguration(any()) } answers { call ->
            val callback = call.invocation.args[0] as ConfigurationCallback
            callback.onResult(configurationSuccess, configurationException)
        }

        every { braintreeClient.sendGraphQLPOST(any(), any()) } answers { call ->
            val callback = call.invocation.args[1] as HttpResponseCallback
            sendGraphQLPostSuccess?.let { callback.onResult(it, null) }
                ?: sendGraphQLPOSTError?.let { callback.onResult(null, it) }
        }

        every { braintreeClient.getReturnUrlScheme() } returns returnUrlScheme

        every {
            braintreeClient.sendPOST(any<String>(), any<String>(), responseCallback = any<HttpResponseCallback>())
        } answers {
            val callback = arg<HttpResponseCallback>(2)
            if (sendPOSTSuccess != null) {
                callback.onResult(sendPOSTSuccess, null)
            } else if (sendPOSTError != null) {
                callback.onResult(null, sendPOSTError)
            }
        }

        every {
            braintreeClient.sendPOST(any<String>(), any<String>(), any<Map<String, String>>(), any<HttpResponseCallback>())
        } answers {
            val callback = arg<HttpResponseCallback>(3)
            if (sendPOSTSuccess != null) {
                callback.onResult(sendPOSTSuccess, null)
            } else if (sendPOSTError != null) {
                callback.onResult(null, sendPOSTError)
            }
        }

        return braintreeClient
    }
}
