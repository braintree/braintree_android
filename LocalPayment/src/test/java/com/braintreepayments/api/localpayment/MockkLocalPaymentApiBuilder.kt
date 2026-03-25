package com.braintreepayments.api.localpayment

import io.mockk.coEvery
import io.mockk.mockk

class MockkLocalPaymentApiBuilder {

    private var tokenizeSuccess: LocalPaymentNonce? = null
    private var tokenizeError: Exception? = null
    private var createPaymentMethodSuccess: LocalPaymentAuthRequestParams? = null
    private var createPaymentMethodError: Exception? = null

    fun tokenizeSuccess(tokenizeSuccess: LocalPaymentNonce) = apply {
        this.tokenizeSuccess = tokenizeSuccess
    }

    fun tokenizeError(error: Exception) = apply {
        this.tokenizeError = error
    }

    fun createPaymentMethodSuccess(createPaymentMethodSuccess: LocalPaymentAuthRequestParams) = apply {
        this.createPaymentMethodSuccess = createPaymentMethodSuccess
    }

    fun createPaymentMethodError(error: Exception) = apply {
        this.createPaymentMethodError = error
    }

    internal fun build(): LocalPaymentApi {
        val localPaymentApi = mockk<LocalPaymentApi>(relaxed = true)

        coEvery {
            localPaymentApi.tokenize(any(), any(), any())
        } answers {
            tokenizeSuccess
                ?: throw (tokenizeError
                    ?: IllegalStateException("No mock result configured for tokenize"))
        }

        coEvery {
            localPaymentApi.createPaymentMethod(any())
        } answers {
            createPaymentMethodSuccess
                ?: throw (createPaymentMethodError
                    ?: IllegalStateException("No mock result configured for createPaymentMethod"))
        }

        return localPaymentApi
    }
}
