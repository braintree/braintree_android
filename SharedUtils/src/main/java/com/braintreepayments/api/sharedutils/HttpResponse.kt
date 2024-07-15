package com.braintreepayments.api.sharedutils

data class HttpResponse(
    val body: String? = null,
    val timing: HttpResponseTiming
)
