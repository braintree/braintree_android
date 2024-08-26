package com.braintreepayments.api.sharedutils

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class Time {

    val currentTime: Long
        get() = System.currentTimeMillis()
}
