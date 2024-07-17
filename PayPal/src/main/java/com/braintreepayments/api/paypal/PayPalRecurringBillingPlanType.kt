package com.braintreepayments.api.paypal

/**
 * PayPal recurring billing plan type, or charge pattern.
 */
enum class PayPalRecurringBillingPlanType {
    /**
     * Variable amount, fixed frequency, no defined duration. (E.g., utility bills, insurance).
     */
    RECURRING,

    /**
     * Fixed amount, fixed frequency, defined duration. (E.g., pay for furniture using monthly payments).
     */
    INSTALLMENT,

    /**
     * Fixed or variable amount, variable freq, no defined duration. (E.g., Coffee shop card reload, prepaid road tolling).
     */
    UNSCHEDULED,

    /**
     * Fixed amount, fixed frequency, no defined duration. (E.g., Streaming service).
     */
    SUBSCRIPTION
}