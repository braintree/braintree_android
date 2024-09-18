package com.braintreepayments.api.sharedutils

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class HttpResponse(
    val body: String? = null,
    val timing: HttpResponseTiming
)
