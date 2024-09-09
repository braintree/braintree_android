package com.braintreepayments.demo

fun interface BraintreeAuthorizationCallback {
    fun onResult(authResult: BraintreeAuthorizationResult)
}
