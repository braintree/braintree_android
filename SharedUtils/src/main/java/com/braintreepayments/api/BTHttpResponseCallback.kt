package com.braintreepayments.api

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface BTHttpResponseCallback {
    @MainThread
    fun onResult(response: BTHttpResponse?, httpError: Exception?)
}
