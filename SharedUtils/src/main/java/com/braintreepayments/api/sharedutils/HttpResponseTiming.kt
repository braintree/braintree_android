package com.braintreepayments.api.sharedutils

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class HttpResponseTiming(
    var startTime: Long,
    var endTime: Long,
)
