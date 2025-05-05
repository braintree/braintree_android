package com.braintreepayments.api.googlepay

import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.core.Configuration
import com.google.android.gms.wallet.IsReadyToPayRequest
import io.mockk.every
import io.mockk.mockk

@Suppress("MagicNumber")
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
            val callback = call.invocation.args[3] as GooglePayIsReadyToPayCallback
            isReadyToPayError?.let { callback.onGooglePayReadinessResult(
                GooglePayReadinessResult.NotReadyToPay(isReadyToPayError!!)
            ) }
            ?: let { callback.onGooglePayReadinessResult(GooglePayReadinessResult.ReadyToPay) }
        }

        return googlePayInternalClient
    }
}
