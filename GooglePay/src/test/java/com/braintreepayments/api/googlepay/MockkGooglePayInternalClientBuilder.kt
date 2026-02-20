package com.braintreepayments.api.googlepay

import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.core.Configuration
import com.google.android.gms.wallet.IsReadyToPayRequest
import io.mockk.every
import io.mockk.mockk

internal class MockkGooglePayInternalClientBuilder {

    private var isReadyToPay: Boolean = false
    private var isReadyToPayError: Exception? = null

    fun isReadyToPay(isReadyToPay: Boolean): MockkGooglePayInternalClientBuilder {
        this.isReadyToPay = isReadyToPay
        return this
    }

    fun isReadyToPayError(isReadyToPayError: Exception): MockkGooglePayInternalClientBuilder {
        this.isReadyToPayError = isReadyToPayError
        return this
    }

    fun build(): GooglePayInternalClient {
        val googlePayInternalClient = mockk<GooglePayInternalClient>(relaxed = true)

        every {
            googlePayInternalClient.isReadyToPay(
                any<FragmentActivity>(),
                any<Configuration>(),
                any<IsReadyToPayRequest>(),
                any<GooglePayIsReadyToPayCallback>()
            )
        } answers { call ->
            val callback =
                call.invocation.args[GOOGLE_PAY_READINESS_CALLBACK_ARG_INDEX] as GooglePayIsReadyToPayCallback
            isReadyToPayError?.let { callback.onGooglePayReadinessResult(
                GooglePayReadinessResult.NotReadyToPay(isReadyToPayError!!)
            ) }
            ?: let { callback.onGooglePayReadinessResult(GooglePayReadinessResult.ReadyToPay) }
        }

        return googlePayInternalClient
    }

    companion object {
        private const val GOOGLE_PAY_READINESS_CALLBACK_ARG_INDEX = 3
    }
}
