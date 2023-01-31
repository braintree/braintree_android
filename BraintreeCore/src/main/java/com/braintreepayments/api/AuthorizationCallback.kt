package com.braintreepayments.api

import androidx.annotation.RestrictTo
import java.lang.Exception

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
interface AuthorizationCallback {
    fun onAuthorizationResult(authorization: Authorization?, error: Exception?)
}