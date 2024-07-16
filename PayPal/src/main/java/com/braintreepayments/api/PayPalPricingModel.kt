package com.braintreepayments.api

/**
 * The interval at which the payment is charged or billed.
 */
enum class PayPalPricingModel {
    FIXED,
    VARIABLE,
    AUTO_RELOAD
}