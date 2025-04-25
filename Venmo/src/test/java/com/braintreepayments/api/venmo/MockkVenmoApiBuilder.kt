package com.braintreepayments.api.venmo

import io.mockk.every
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

        every { venmoApi.createPaymentContext(
            any<VenmoRequest>(),
            any<String>(),
            any<VenmoApiCallback>())
        } answers { call ->
            var callback = call.invocation.args[2] as VenmoApiCallback
            if (venmoPaymentContextId != null) {
                callback.onResult(venmoPaymentContextId, null)
            } else if (createPaymentContextError != null) {
                callback.onResult(null, createPaymentContextError)
            }
        }

        every { venmoApi.createNonceFromPaymentContext(
            any<String>(),
            any<VenmoInternalCallback>())
        } answers { call ->
            var callback = call.invocation.args[1] as VenmoInternalCallback
            if (createNonceFromPaymentContextSuccess != null) {
                callback.onResult(createNonceFromPaymentContextSuccess, null)
            } else if (createNonceFromPaymentContextError != null) {
                callback.onResult(null, createNonceFromPaymentContextError)
            }
        }

        every { venmoApi.vaultVenmoAccountNonce(
            any<String>(),
            any<VenmoInternalCallback>())
        } answers { call ->
            var callback = call.invocation.args[1] as VenmoInternalCallback
            if (vaultVenmoAccountNonceSuccess != null) {
                callback.onResult(vaultVenmoAccountNonceSuccess, null)
            } else if (vaultVenmoAccountNonceError != null) {
                callback.onResult(null, vaultVenmoAccountNonceError)
            }
        }

        return venmoApi
    }
}
