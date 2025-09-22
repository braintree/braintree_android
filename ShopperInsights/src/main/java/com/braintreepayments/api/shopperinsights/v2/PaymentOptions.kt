package com.braintreepayments.api.shopperinsights.v2

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * A single payment recommendation
 *
 * @property paymentOption The payment option type
 * @property recommendedPriority The rank of the payment option
 *
 * Warning: This feature is in beta. It's public API may change or be removed in future releases.
 */
@ExperimentalBetaApi
data class PaymentOptions(
    val paymentOption: String,
    val recommendedPriority: Int
)
