package com.braintreepayments.api.card.paymentactions

import androidx.annotation.RestrictTo

/**
 * Payment Actions Analytics Keys.
 *
 * TODO: These keys are currently placeholder values that must be changed when we get the official values.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object CardPaymentActionsAnalytics {
    const val SET_PAYMENT_METHOD_STARTED = "payment-actions:set-payment-method:started"
    const val SET_PAYMENT_METHOD_SUCCEEDED = "payment-actions:set-payment-method:succeeded"
    const val SET_PAYMENT_METHOD_FAILED = "payment-actions:set-payment-method:failed"
    const val READY_FOR_CONFIRMATION = "payment-actions:ready-for-confirmation"
}