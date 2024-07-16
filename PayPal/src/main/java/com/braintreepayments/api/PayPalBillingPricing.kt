package com.braintreepayments.api

data class PayPalBillingPricing(
    var pricingModel: PayPalPricingModel,
    var amount: String,
    var reloadThresholdAmount: String?
)


