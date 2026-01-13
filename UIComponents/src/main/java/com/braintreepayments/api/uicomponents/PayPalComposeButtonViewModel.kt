package com.braintreepayments.api.uicomponents

import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalTokenizeCallback

class PayPalComposeButtonViewModel: ViewModel() {

    private var pendingRequest: String? = null

    fun storePayPalPendingRequest(request: PayPalPendingRequest.Started) {
        pendingRequest = request.pendingRequestString
    }

    fun getPayPalPendingRequest(): PayPalPendingRequest.Started {
        return PayPalPendingRequest.Started(pendingRequest ?: "")
    }

    fun clearPayPalPendingRequest() {
        pendingRequest = null
    }

    fun handleReturnToApp(
        payPalLauncher: PayPalLauncher,
        payPalClient: PayPalClient,
        pendingRequest: PayPalPendingRequest.Started,
        intent: Intent,
        callback: PayPalTokenizeCallback
    ) {
        if (pendingRequest.pendingRequestString.isEmpty()) return
        val paymentAuthResult = payPalLauncher.handleReturnToApp(
            pendingRequest = pendingRequest,
            intent = intent,
        )

        when (paymentAuthResult) {
            is PayPalPaymentAuthResult.Success -> {
                payPalClient.tokenize(paymentAuthResult) { payPalResult ->
                    callback.onPayPalResult(payPalResult)
                }
            }
            is PayPalPaymentAuthResult.NoResult -> {
                callback.onPayPalResult(PayPalResult.Cancel)
            }
            is PayPalPaymentAuthResult.Failure -> {
                callback.onPayPalResult(PayPalResult.Failure(paymentAuthResult.error))
            }
        }
    }
}
