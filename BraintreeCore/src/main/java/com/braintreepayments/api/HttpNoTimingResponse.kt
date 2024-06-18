package com.braintreepayments.api

internal class HttpNoTimingResponse : HttpTimingResponseCallback {
    override fun onResult(response: HttpTimingResponse?, httpError: Exception?) {
        // No-op
    }
}
