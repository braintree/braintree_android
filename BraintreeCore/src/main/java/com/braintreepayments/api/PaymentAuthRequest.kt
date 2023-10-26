package com.braintreepayments.api

sealed class PaymentAuthRequest {

    class Ready(val launchRequest: LaunchRequest): PaymentAuthRequest()

    class Failure(val error: Exception) : PaymentAuthRequest()

}