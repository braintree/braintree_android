package com.braintreepayments.api

internal enum class AmericanExpressAnalytics(@JvmField val event: String) {

    REWARDS_BALANCE_STARTED("amex:rewards-balance:started"),
    REWARDS_BALANCE_FAILED("amex:rewards-balance:failed"),
    REWARDS_BALANCE_SUCCEEDED("amex:rewards-balance:succeeded")
}
