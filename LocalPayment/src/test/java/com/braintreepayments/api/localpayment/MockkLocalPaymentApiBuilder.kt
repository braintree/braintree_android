package com.braintreepayments.api.localpayment

import io.mockk.every
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

        every {
            localPaymentApi.tokenize(any(), any(), any(), any())
        } answers {
            val callback = arg<LocalPaymentInternalTokenizeCallback>(3)
            when {
                tokenizeSuccess != null -> callback.onResult(tokenizeSuccess, null)
                tokenizeError != null -> callback.onResult(null, tokenizeError)
            }
        }

        every {
            localPaymentApi.createPaymentMethod(any(), any())
        } answers {
            val callback = arg<LocalPaymentInternalAuthRequestCallback>(1)
            when {
                createPaymentMethodSuccess != null -> callback.onLocalPaymentInternalAuthResult(createPaymentMethodSuccess, null)
                createPaymentMethodError != null -> callback.onLocalPaymentInternalAuthResult(null, createPaymentMethodError)
            }
        }

        return localPaymentApi
    }
}