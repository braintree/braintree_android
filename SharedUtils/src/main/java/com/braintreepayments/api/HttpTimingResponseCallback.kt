package com.braintreepayments.api

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface HttpTimingResponseCallback {
    @MainThread
    fun onResult(response: HttpResponse?, httpError: Exception?)
}
