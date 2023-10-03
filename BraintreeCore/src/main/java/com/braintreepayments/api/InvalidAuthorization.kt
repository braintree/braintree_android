package com.braintreepayments.api

import androidx.annotation.RestrictTo

/**
 * @suppress
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class InvalidAuthorization(rawValue: String, val errorMessage: String) :
    Authorization(rawValue) {

    override val configUrl: String? = null
    override val bearer: String? = null
}
