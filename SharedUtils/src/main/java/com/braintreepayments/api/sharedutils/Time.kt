package com.braintreepayments.api.sharedutils

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class Time {

    /**
     * Returns the current time in milliseconds
     */
    val currentTime: Long
        get() = System.currentTimeMillis()
}
