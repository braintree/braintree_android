package com.braintreepayments.api.core

import androidx.annotation.RestrictTo

/**
 * Error class thrown when a user cancels a payment flow
 */
class UserCanceledException @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor(
    message: String?,
) : BraintreeException(message)
