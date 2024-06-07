package com.braintreepayments.api

internal class BTHttpNoResponse: BTHttpResponseCallback {

    override fun onResult(response: BTHttpResponse?, httpError: Exception?) {
    }
}