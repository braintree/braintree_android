package com.braintreepayments.api

import io.mockk.every
import io.mockk.mockk

internal class MockkPayPalInternalClientBuilder {

    private var error: Exception? = null
    private var successResponse: PayPalNativeCheckoutResponse? = null
    private var tokenizeSuccess: PayPalNativeCheckoutAccountNonce? = null

    fun sendRequestSuccess(successResponse: PayPalNativeCheckoutResponse): MockkPayPalInternalClientBuilder {
        this.successResponse = successResponse
        return this
    }

    fun sendRequestError(error: Exception): MockkPayPalInternalClientBuilder {
        this.error = error
        return this
    }

    fun tokenizeSuccess(tokenizeSuccess: PayPalNativeCheckoutAccountNonce): MockkPayPalInternalClientBuilder {
        this.tokenizeSuccess = tokenizeSuccess
        return this
    }

    fun build(): PayPalNativeCheckoutInternalClient {
        val payPalInternalClient = mockk<PayPalNativeCheckoutInternalClient>(relaxed = true)

        every { payPalInternalClient.sendRequest(any(), any(), any()) } answers {
            val callback = lastArg() as PayPalNativeCheckoutInternalClientCallback
            successResponse?.let {
                callback.onResult(it, null)
            } ?: error?.let {
                callback.onResult(null, it)
            }
        }

        every { payPalInternalClient.tokenize(any(), any()) } answers {
            val callback = lastArg() as PayPalNativeCheckoutResultCallback
            callback.onResult(tokenizeSuccess, null)
        }

        return payPalInternalClient
    }
}
