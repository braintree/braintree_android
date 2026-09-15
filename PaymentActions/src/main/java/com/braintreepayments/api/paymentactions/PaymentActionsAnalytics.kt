package com.braintreepayments.api.paymentactions

internal object PaymentActionsAnalytics {
    const val SET_PAYMENT_ACTION_PAYMENT_METHOD_STARTED =
        "payment-actions:set-payment-action-payment-method:started"
    const val SET_PAYMENT_ACTION_PAYMENT_METHOD_SUCCEEDED =
        "payment-actions:set-payment-action-payment-method:succeeded"
    const val SET_PAYMENT_ACTION_PAYMENT_METHOD_FAILED =
        "payment-actions:set-payment-action-payment-method:failed"
}
