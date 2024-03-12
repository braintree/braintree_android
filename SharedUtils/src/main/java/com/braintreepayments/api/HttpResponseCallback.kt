package com.braintreepayments.api

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface HttpResponseCallback {

    @MainThread
    fun onResult(responseBody: String?, httpError: Exception?)
}
