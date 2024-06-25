package com.braintreepayments.api

data class HttpResponse(
    var body: String? = null,
    var timing: HttpResponseTiming
)
