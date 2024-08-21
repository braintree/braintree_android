package com.braintreepayments.api.localpayment

import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock

class MockLocalPaymentApiBuilder {
    private var tokenizeSuccess: LocalPaymentNonce? = null
    private var tokenizeError: Exception? = null
    private var createPaymentMethodSuccess: LocalPaymentAuthRequestParams? = null
    private var createPaymentMethodError: Exception? = null

    fun tokenizeSuccess(tokenizeSuccess: LocalPaymentNonce?): MockLocalPaymentApiBuilder {
        this.tokenizeSuccess = tokenizeSuccess
        return this
    }

    fun tokenizeError(error: Exception?): MockLocalPaymentApiBuilder {
        this.tokenizeError = error
        return this
    }

    fun createPaymentMethodSuccess(
        createPaymentMethodSuccess: LocalPaymentAuthRequestParams?
    ): MockLocalPaymentApiBuilder {
        this.createPaymentMethodSuccess = createPaymentMethodSuccess
        return this
    }

    fun createPaymentMethodError(error: Exception?): MockLocalPaymentApiBuilder {
        this.createPaymentMethodError = error
        return this
    }

    internal fun build(): LocalPaymentApi {
        val localPaymentAPI = Mockito.mock(LocalPaymentApi::class.java)

        Mockito.doAnswer { invocation: InvocationOnMock ->
            val callback =
                invocation.arguments[3] as LocalPaymentInternalTokenizeCallback
            if (tokenizeSuccess != null) {
                callback.onResult(tokenizeSuccess, null)
            } else if (tokenizeError != null) {
                callback.onResult(null, tokenizeError)
            }
            null
        }.`when`(localPaymentAPI).tokenize(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(
                LocalPaymentInternalTokenizeCallback::class.java
            )
        )

        Mockito.doAnswer { invocation: InvocationOnMock ->
            val callback =
                invocation.arguments[1] as LocalPaymentInternalAuthRequestCallback
            if (createPaymentMethodSuccess != null) {
                callback.onLocalPaymentInternalAuthResult(createPaymentMethodSuccess, null)
            } else if (createPaymentMethodError != null) {
                callback.onLocalPaymentInternalAuthResult(null, createPaymentMethodError)
            }
            null
        }.`when`(localPaymentAPI).createPaymentMethod(
            ArgumentMatchers.any(
                LocalPaymentRequest::class.java
            ),
            ArgumentMatchers.any(
                LocalPaymentInternalAuthRequestCallback::class.java
            )
        )

        return localPaymentAPI
    }
}
