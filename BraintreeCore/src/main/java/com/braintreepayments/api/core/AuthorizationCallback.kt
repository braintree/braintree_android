package com.braintreepayments.api.core

import androidx.annotation.RestrictTo

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun interface AuthorizationCallback {
    fun onAuthorizationResult(authorization: Authorization?, error: Exception?)
}
