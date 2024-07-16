package com.braintreepayments.api

data class PayPalBillingCycle(
    var interval: PayPalBillingInterval,
    var intervalCount: Int,
    var numberOfExecutions: Int,
    var sequence: Int?,
    var startDate: String?,
    var isTrial: Boolean,
    var pricing: PayPalBillingPricing?
)
