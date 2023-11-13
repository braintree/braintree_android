package com.braintreepayments.api

sealed class VenmoPaymentAuthRequest {

    class ReadyToLaunch(val requestParams: VenmoPaymentAuthRequestParams): VenmoPaymentAuthRequest()

    class Failure(val error: Exception) : VenmoPaymentAuthRequest()

}

