package com.braintreepayments.api.paypal

import android.content.Context
import io.mockk.every
import io.mockk.mockk

class MockkPayPalInternalClientBuilder {

    private var error: Exception? = null
    private var successResponse: PayPalPaymentAuthRequestParams? = null
    private var tokenizeSuccess: PayPalAccountNonce? = null

    fun sendRequestSuccess(successResponse: PayPalPaymentAuthRequestParams?): MockkPayPalInternalClientBuilder {
        this.successResponse = successResponse
        return this
    }

    fun sendRequestError(error: Exception?): MockkPayPalInternalClientBuilder {
        this.error = error;
        return this;
    }

    fun tokenizeSuccess(tokenizeSuccess: PayPalAccountNonce): MockkPayPalInternalClientBuilder {
        this.tokenizeSuccess = tokenizeSuccess;
        return this;
    }

    internal fun build(): PayPalInternalClient {
        val payPalInternalClient = mockk<PayPalInternalClient>(relaxed = true)

        every {
            payPalInternalClient.sendRequest(
                any<Context>(),
                any<PayPalRequest>(),
                any<PayPalInternalClientCallback>()
            )
        } answers {
            val callback = invocation.args[2] as PayPalInternalClientCallback
            if (successResponse != null) {
                callback.onResult(successResponse, null);
            } else if (error != null) {
                callback.onResult(null, error);
            }
            null
        }

        every {
            payPalInternalClient.tokenize(
                any<PayPalAccount>(),
                any<PayPalInternalTokenizeCallback>()
            )
        } answers {
            val callback = invocation.args[1] as PayPalInternalTokenizeCallback
            callback.onResult(tokenizeSuccess, null)
            null
        }

        return payPalInternalClient
    }
}
