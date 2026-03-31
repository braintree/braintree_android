package com.braintreepayments.api.venmo

import io.mockk.coEvery
import io.mockk.mockk

internal class MockkVenmoApiBuilder {

    private var venmoPaymentContextId: String? = null
    private var createNonceFromPaymentContextSuccess: VenmoAccountNonce? = null
    private var vaultVenmoAccountNonceSuccess: VenmoAccountNonce? = null
    private var createPaymentContextError: Exception? = null
    private var createNonceFromPaymentContextError: Exception? = null
    private var vaultVenmoAccountNonceError: Exception? = null

    fun createPaymentContextSuccess(venmoPaymentContextId: String): MockkVenmoApiBuilder {
        this.venmoPaymentContextId = venmoPaymentContextId
        return this
    }

    fun createPaymentContextError(createPaymentContextError: Exception): MockkVenmoApiBuilder {
        this.createPaymentContextError = createPaymentContextError
        return this
    }

    fun createNonceFromPaymentContextSuccess(
        createNonceFromPaymentContextSuccess: VenmoAccountNonce
    ): MockkVenmoApiBuilder {
        this.createNonceFromPaymentContextSuccess = createNonceFromPaymentContextSuccess
        return this
    }

    fun createNonceFromPaymentContextError(
        createNonceFromPaymentContextError: Exception
    ): MockkVenmoApiBuilder {
        this.createNonceFromPaymentContextError = createNonceFromPaymentContextError
        return this
    }

    fun vaultVenmoAccountNonceSuccess(
        vaultVenmoAccountNonceSuccess: VenmoAccountNonce
    ): MockkVenmoApiBuilder {
        this.vaultVenmoAccountNonceSuccess = vaultVenmoAccountNonceSuccess
        return this
    }

    fun vaultVenmoAccountNonceError(vaultVenmoAccountNonceError: Exception): MockkVenmoApiBuilder {
        this.vaultVenmoAccountNonceError = vaultVenmoAccountNonceError
        return this
    }

    fun build(): VenmoApi {
        val venmoApi = mockk<VenmoApi>(relaxed = true)

        coEvery {
            venmoApi.createPaymentContext(any(), any())
        } answers {
            venmoPaymentContextId?.let { return@answers it }
            createPaymentContextError?.let { throw it }
            throw IllegalStateException("No mock result configured for createPaymentContext")
        }

        coEvery {
            venmoApi.createNonceFromPaymentContext(any())
        } answers {
            createNonceFromPaymentContextSuccess?.let { return@answers it }
            createNonceFromPaymentContextError?.let { throw it }
            throw IllegalStateException("No mock result configured for createNonceFromPaymentContext")
        }

        coEvery {
            venmoApi.vaultVenmoAccountNonce(any())
        } answers {
            vaultVenmoAccountNonceSuccess?.let { return@answers it }
            vaultVenmoAccountNonceError?.let { throw it }
            throw IllegalStateException("No mock result configured for vaultVenmoAccountNonce")
        }

        return venmoApi
    }
}
