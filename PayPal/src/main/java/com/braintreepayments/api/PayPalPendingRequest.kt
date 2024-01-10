package com.braintreepayments.api

sealed class PayPalPendingRequest {

    class Success(val request: PayPalBrowserSwitchRequest) : PayPalPendingRequest()

    class Failure(val error: Exception) : PayPalPendingRequest()
}