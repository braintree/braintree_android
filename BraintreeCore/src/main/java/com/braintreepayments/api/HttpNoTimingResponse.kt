package com.braintreepayments.api

internal class HttpNoTimingResponse : HttpTimingResponseCallback {
    override fun onResult(response: HttpResponse?, httpError: Exception?) {
        // No-op
    }
}
