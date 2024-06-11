package com.braintreepayments.api

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface BTHttpResponseCallback {
    @MainThread
    fun onResult(response: BTHttpResponse?, httpError: Exception?)
}
