package com.braintreepayments.api

import io.mockk.every
import io.mockk.mockk

class MockkAuthorizationProviderBuilder {

    private var error: Exception? = null
    private var clientTokens: MutableList<String>? = null

    fun clientToken(vararg clientTokens: String): MockkAuthorizationProviderBuilder {
        this.clientTokens = clientTokens.toMutableList()
        return this
    }

    fun error(error: Exception): MockkAuthorizationProviderBuilder {
        this.error = error
        return this
    }

    fun build(): ClientTokenProvider {
        val clientTokenProvider = mockk<ClientTokenProvider>()

        every { clientTokenProvider.getClientToken(any()) } answers {
            // shift array until one item left, at which point all subsequent calls
            // will return the last item
            val clientToken =
                clientTokens?.let { if (it.size > 1) it.removeFirst() else it.first() }

            val callback = firstArg<ClientTokenCallback>()
            clientToken?.let {
                callback.onSuccess(it)
            } ?: error?.let {
                callback.onFailure(it)
            }
        }

        return clientTokenProvider
    }
}