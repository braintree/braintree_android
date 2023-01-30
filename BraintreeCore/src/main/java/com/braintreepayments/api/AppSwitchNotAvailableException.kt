package com.braintreepayments.api

/**
 * Error class thrown when app switch to the corresponding wallet is not possible
 */
open class AppSwitchNotAvailableException internal constructor(message: String?) :
    BraintreeException(message)
