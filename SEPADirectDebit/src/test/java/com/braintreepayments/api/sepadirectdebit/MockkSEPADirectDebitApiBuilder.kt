package com.braintreepayments.api.sepadirectdebit

import io.mockk.coEvery
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

    @Suppress("ThrowsCount")
    internal fun build(): SEPADirectDebitApi {
        val api = mockk<SEPADirectDebitApi>(relaxed = true)

        coEvery {
            api.createMandate(any(), any())
        } answers {
            createMandateResultSuccess?.let { return@answers it }
            createMandateError?.let { throw it }
            throw IllegalStateException("No mock result configured for createMandate")
        }

        coEvery {
            api.tokenize(any(), any(), any(), any())
        } answers {
            tokenizeSuccess?.let { return@answers it }
            tokenizeError?.let { throw it }
            throw IllegalStateException("No mock result configured for tokenize")
        }

        return api
    }
}
