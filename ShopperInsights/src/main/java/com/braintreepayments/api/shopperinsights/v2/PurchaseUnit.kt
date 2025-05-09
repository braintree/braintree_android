package com.braintreepayments.api.shopperinsights.v2

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * Amounts of the items purchased.
 *
 * @property amount The amount of money, either a whole number or a number with up to 3 decimal places.
 * @property currencyCode The currency code for the monetary amount.
 */
@ExperimentalBetaApi
data class PurchaseUnit(
    val amount: String,
    val currencyCode: String,
)
