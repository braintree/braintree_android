package com.braintreepayments.api

import androidx.annotation.RestrictTo

/**
 * Error class thrown when app switch to the corresponding wallet is not possible
 */
open class AppSwitchNotAvailableException @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor(
    message: String?
) : BraintreeException(message)
