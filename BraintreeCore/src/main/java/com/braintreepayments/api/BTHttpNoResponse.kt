package com.braintreepayments.api

internal class BTHttpNoResponse : BTHttpResponseCallback {
    override fun onResult(response: HttpTimingResponse?, httpError: Exception?) {
        // No-op
    }
}
