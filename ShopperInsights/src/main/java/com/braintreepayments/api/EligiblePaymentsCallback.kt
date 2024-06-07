package com.braintreepayments.api

import java.lang.Exception

/**
 * A callback that returns information on whether someone is a PayPal or a Venmo shopper.
 */
internal fun interface EligiblePaymentsCallback {
    fun onResult(result: EligiblePaymentsApiResult?, error: Exception?)
}
