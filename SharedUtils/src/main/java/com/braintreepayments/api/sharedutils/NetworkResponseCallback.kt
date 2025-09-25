package com.braintreepayments.api.sharedutils

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface NetworkResponseCallback {

    sealed class Result {
        data class Success(val response: HttpResponse) : Result()
        data class Failure(val error: Exception) : Result()
    }

    @MainThread
    fun onResult(result: Result)
}
