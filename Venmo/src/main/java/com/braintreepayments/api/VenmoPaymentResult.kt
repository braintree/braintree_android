package com.braintreepayments.api

sealed class VenmoPaymentResult {

    class Success(val nonce: VenmoAccountNonce): VenmoPaymentResult()

    class Failure(val error: Exception) : VenmoPaymentResult()

    object Cancel : VenmoPaymentResult()

}