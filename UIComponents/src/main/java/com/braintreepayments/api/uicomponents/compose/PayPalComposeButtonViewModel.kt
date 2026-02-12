package com.braintreepayments.api.uicomponents.compose

import androidx.lifecycle.ViewModel

internal class PayPalComposeButtonViewModel(
    val repository: PayPalPendingRequestRepository
): ViewModel() {

//    fun getPayPalPendingRequest(): PayPalPendingRequest.Started {
//        return PayPalPendingRequest.Started(repository.pendingRequest ?: "")
//    }
//
//    fun storePayPalPendingRequest(request: PayPalPendingRequest.Started) {
//        repository.pendingRequest = request.pendingRequestString
//    }
//
//    fun clearPayPalPendingRequest() {
//        repository.pendingRequest = null
//    }
//
//    fun handleReturnToApp(
//        payPalLauncher: PayPalLauncher,
//        payPalClient: PayPalClient,
//        pendingRequest: PayPalPendingRequest.Started,
//        intent: Intent,
//        callback: PayPalTokenizeCallback
//    ) {
//        val paymentAuthResult = payPalLauncher.handleReturnToApp(
//            pendingRequest = pendingRequest,
//            intent = intent,
//        )
//
//        when (paymentAuthResult) {
//            is PayPalPaymentAuthResult.Success -> {
//                payPalClient.tokenize(paymentAuthResult) { payPalResult ->
//                    callback.onPayPalResult(payPalResult)
//                }
//            }
//            is PayPalPaymentAuthResult.NoResult -> {
//                callback.onPayPalResult(PayPalResult.Cancel)
//            }
//            is PayPalPaymentAuthResult.Failure -> {
//                callback.onPayPalResult(PayPalResult.Failure(paymentAuthResult.error))
//            }
//        }
//    }
}
