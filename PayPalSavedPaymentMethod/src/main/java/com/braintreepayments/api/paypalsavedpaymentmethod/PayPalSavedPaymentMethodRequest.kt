package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi

/**
 * The inputs the PayPal Saved Payment Method component needs to resolve the buyer's saved funding instrument and its
 * accompanying Pay Later message.
 *
 * [amount] and [currencyCode] must match the `PayPalCheckoutRequest` passed to the component. The SDK doesn't check
 * this, so a mismatch shows a Pay Later message quoted for a different amount.
 *
 * @param amount The order amount the Pay Later message is calculated from, for example `"55.00"`.
 * @param currencyCode The three-character ISO-4217 currency code for [amount].
 * @param merchantAccountId Optional. A non-default merchant account to resolve the funding instrument against.
 */
@ExperimentalBetaApi
class PayPalSavedPaymentMethodRequest @JvmOverloads constructor(
    internal val amount: String,
    internal val currencyCode: String,
    internal val merchantAccountId: String? = null
)
