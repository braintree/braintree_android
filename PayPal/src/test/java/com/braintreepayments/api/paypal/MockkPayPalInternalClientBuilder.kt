package com.braintreepayments.api.paypal

import android.content.Context
import com.braintreepayments.api.core.Configuration
import io.mockk.coEvery
import io.mockk.mockk

@Suppress("ThrowsCount")
class MockkPayPalInternalClientBuilder {

    private var error: Exception? = null
    private var successResponse: PayPalPaymentAuthRequestParams? = null
    private var tokenizeSuccess: PayPalAccountNonce? = null

    fun sendRequestSuccess(successResponse: PayPalPaymentAuthRequestParams?): MockkPayPalInternalClientBuilder {
        this.successResponse = successResponse
        return this
    }

    fun sendRequestError(error: Exception?): MockkPayPalInternalClientBuilder {
        this.error = error
        return this
    }

    fun tokenizeSuccess(tokenizeSuccess: PayPalAccountNonce): MockkPayPalInternalClientBuilder {
        this.tokenizeSuccess = tokenizeSuccess
        return this
    }

    internal fun build(): PayPalInternalClient {
        val payPalInternalClient = mockk<PayPalInternalClient>(relaxed = true)

        coEvery {
            payPalInternalClient.sendRequest(
                any<Context>(),
                any<PayPalRequest>(),
                any<Configuration>()
            )
        } answers {
            successResponse?.let { return@answers it }
            error?.let { throw it }
            throw IllegalStateException("No mock result configured for sendRequest")
        }

        coEvery {
            payPalInternalClient.tokenize(
                any<PayPalAccount>()
            )
        } answers {
            tokenizeSuccess
                ?: throw IllegalStateException("No mock result configured for tokenize")
        }

        return payPalInternalClient
    }
}
