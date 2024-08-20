package com.braintreepayments.api.googlepay

import android.content.Context
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.googlepay.GooglePayReadinessResult.NotReadyToPay
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.Wallet.WalletOptions
import com.google.android.gms.wallet.WalletConstants

internal class GooglePayInternalClient {
    fun isReadyToPay(
        context: Context,
        configuration: Configuration,
        isReadyToPayRequest: IsReadyToPayRequest,
        callback: GooglePayIsReadyToPayCallback
    ) {
        val paymentsClient = Wallet.getPaymentsClient(
            context,
            WalletOptions.Builder()
                .setEnvironment(getGooglePayEnvironment(configuration))
                .build()
        )
        paymentsClient.isReadyToPay(isReadyToPayRequest)
            .addOnCompleteListener { task: Task<Boolean> ->
                try {
                    val isReady = task.getResult(ApiException::class.java)
                    if (isReady) {
                        callback.onGooglePayReadinessResult(GooglePayReadinessResult.ReadyToPay)
                    } else {
                        callback.onGooglePayReadinessResult(NotReadyToPay(null))
                    }
                } catch (e: ApiException) {
                    callback.onGooglePayReadinessResult(NotReadyToPay(e))
                }
            }
    }

    private fun getGooglePayEnvironment(configuration: Configuration): Int {
        return if ("production" == configuration.googlePayEnvironment) {
            WalletConstants.ENVIRONMENT_PRODUCTION
        } else {
            WalletConstants.ENVIRONMENT_TEST
        }
    }
}
