package com.braintreepayments.api.testutils

import android.content.pm.ActivityInfo
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.sharedutils.HttpResponseCallback
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.io.IOException

@Suppress("MagicNumber", "TooManyFunctions")
class MockkBraintreeClientBuilder {

    private var sendGraphQLPostSuccess: String? = null
    private var sendGraphQLPostError: Exception? = null

    private var sendGetSuccess: String? = null
    private var sendGetError: Exception? = null

    private var sendPostSuccess: String? = null
    private var sendPostError: Exception? = null

    private var configurationSuccess: Configuration? = null
    private var configurationException: Exception? = null
    private var authorizationSuccess: Authorization? = null
    private var activityInfo: ActivityInfo? = null

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

    fun activityInfo(activityInfo: ActivityInfo): MockkBraintreeClientBuilder {
        this.activityInfo = activityInfo
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

    fun sendGraphQLPostSuccessfulResponse(sendGraphQLPostSuccess: String): MockkBraintreeClientBuilder {
        this.sendGraphQLPostSuccess = sendGraphQLPostSuccess
        return this
    }

    fun sendGraphQLPostErrorResponse(sendGraphQLPostError: Exception?): MockkBraintreeClientBuilder {
        this.sendGraphQLPostError = sendGraphQLPostError
        return this
    }

    fun sendPostSuccessfulResponse(sendPostSuccess: String): MockkBraintreeClientBuilder {
        this.sendPostSuccess = sendPostSuccess
        return this
    }

    fun sendPostErrorResponse(sendPostError: Exception?): MockkBraintreeClientBuilder {
        this.sendPostError = sendPostError
        return this
    }

    fun sendGetSuccessfulResponse(sendGetSuccess: String): MockkBraintreeClientBuilder {
        this.sendGetSuccess = sendGetSuccess
        return this
    }

    fun sendGetErrorResponse(sendGetError: Exception?): MockkBraintreeClientBuilder {
        this.sendGetError = sendGetError
        return this
    }

    @Suppress("ThrowsCount")
    fun build(): BraintreeClient {
        val braintreeClient = mockk<BraintreeClient>(relaxed = true)

        every { braintreeClient.launchesBrowserSwitchAsNewTask() } returns launchesBrowserSwitchAsNewTask

        coEvery { braintreeClient.getConfiguration() } answers {
            configurationSuccess ?: throw (configurationException ?: IOException("No configuration configured"))
        }

        every { braintreeClient.sendGraphQLPOST(any(), any()) } answers { call ->
            val callback = call.invocation.args[1] as HttpResponseCallback
            sendGraphQLPostSuccess?.let { callback.onResult(it, null) }
                ?: sendGraphQLPostError?.let { callback.onResult(null, it) }
        }

        every { braintreeClient.getReturnUrlScheme() } returns returnUrlScheme

        every { braintreeClient.getManifestActivityInfo(any<Class<*>>()) } returns activityInfo

        val sendPostAnswer: () -> String = {
            sendPostSuccess ?: throw (sendPostError ?: IOException("Unknown error"))
        }

        coEvery {
            braintreeClient.sendPOST(
                url = any<String>(),
                data = any<String>(),
            )
        } answers { sendPostAnswer() }

        coEvery {
            braintreeClient.sendPOST(
                url = any<String>(),
                data = any<String>(),
                additionalHeaders = any<Map<String, String>>(),
            )
        } answers { sendPostAnswer() }

        coEvery { braintreeClient.sendGET(any<String>()) } answers {
            sendGetSuccess
                ?: sendGetError?.let { throw it }
                ?: throw IOException("No sendGet response configured")
        }

        return braintreeClient
    }
}
