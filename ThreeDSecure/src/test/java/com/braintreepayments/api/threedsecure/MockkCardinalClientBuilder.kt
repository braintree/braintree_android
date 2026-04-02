package com.braintreepayments.api.threedsecure

import android.content.Context
import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.Configuration
import io.mockk.coEvery
import io.mockk.mockk

internal class MockkCardinalClientBuilder {

    private var error: Exception? = null
    private var initializeRuntimeError: BraintreeException? = null
    private var successReferenceId: String? = null

    fun successReferenceId(successReferenceId: String): MockkCardinalClientBuilder {
        this.successReferenceId = successReferenceId
        return this
    }

    fun error(error: Exception): MockkCardinalClientBuilder {
        this.error = error
        return this
    }

    fun initializeRuntimeError(initializeRuntimeError: BraintreeException): MockkCardinalClientBuilder {
        this.initializeRuntimeError = initializeRuntimeError
        return this
    }

    fun build(): CardinalClient {
        val cardinalClient = mockk<CardinalClient>(relaxed = true)

        coEvery { cardinalClient.consumerSessionId } returns successReferenceId

        if (initializeRuntimeError != null) {
            coEvery {
                cardinalClient.initialize(
                    any<Context>(),
                    any<Configuration>(),
                    any<ThreeDSecureRequest>()
                )
            } throws initializeRuntimeError!!
        } else {
            coEvery {
                cardinalClient.initialize(
                    any<Context>(),
                    any<Configuration>(),
                    any<ThreeDSecureRequest>()
                )
            } returns successReferenceId
        }

        return cardinalClient
    }
}
