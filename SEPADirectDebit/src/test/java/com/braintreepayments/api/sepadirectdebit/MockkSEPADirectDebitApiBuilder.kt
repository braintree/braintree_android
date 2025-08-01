package com.braintreepayments.api.sepadirectdebit

import io.mockk.every
import io.mockk.mockk

class MockkSEPADirectDebitApiBuilder {
    private var createMandateError: Exception? = null
    private var createMandateResultSuccess: CreateMandateResult? = null
    private var tokenizeError: Exception? = null
    private var tokenizeSuccess: SEPADirectDebitNonce? = null

    fun createMandateResultSuccess(createMandateResultSuccess: CreateMandateResult?): MockkSEPADirectDebitApiBuilder {
        this.createMandateResultSuccess = createMandateResultSuccess
        return this
    }

    fun createMandateError(createMandateError: Exception?): MockkSEPADirectDebitApiBuilder {
        this.createMandateError = createMandateError
        return this
    }

    fun tokenizeSuccess(tokenizeSuccess: SEPADirectDebitNonce?): MockkSEPADirectDebitApiBuilder {
        this.tokenizeSuccess = tokenizeSuccess
        return this
    }

    fun tokenizeError(tokenizeError: Exception?): MockkSEPADirectDebitApiBuilder {
        this.tokenizeError = tokenizeError
        return this
    }

    internal fun build(): SEPADirectDebitApi {
        val api = mockk<SEPADirectDebitApi>(relaxed = true)

        every {
            api.createMandate(any(), any(), any())
        } answers {
            val callback = arg<CreateMandateCallback>(2)
            if (createMandateResultSuccess != null) {
                callback.onResult(createMandateResultSuccess, null)
            } else if (createMandateError != null) {
                callback.onResult(null, createMandateError)
            }
        }

        every {
            api.tokenize(any(), any(), any(), any(), any())
        } answers {
            val callback = arg<SEPADirectDebitInternalTokenizeCallback>(4)
            if (tokenizeSuccess != null) {
                callback.onResult(tokenizeSuccess, null)
            } else if (tokenizeError != null) {
                callback.onResult(null, tokenizeError)
            }
        }

        return api
    }
}
