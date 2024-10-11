package com.braintreepayments.demo

sealed class FetchClientTokenResult {
    class Success(val clientToken: String) : FetchClientTokenResult()
    class Error(val error: Exception) : FetchClientTokenResult()
}
