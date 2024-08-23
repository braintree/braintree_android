package com.braintreepayments.demo

sealed class BraintreeAuthorizationResult {
    class Success(val authString: String) : BraintreeAuthorizationResult()
    class Error(val error: Exception) : BraintreeAuthorizationResult()
}
