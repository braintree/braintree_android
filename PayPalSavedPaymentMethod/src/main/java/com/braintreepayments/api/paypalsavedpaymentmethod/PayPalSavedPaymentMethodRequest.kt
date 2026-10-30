package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi
import java.util.Objects

/**
 * The inputs the PayPal Saved Payment Method component needs to resolve the buyer's saved funding instrument and its
 * accompanying Pay Later message.
 *
 * Values must match the `PayPalCheckoutRequest` passed to the component; the merchant keeps them in sync. If they
 * differ, the Pay Later message is quoted for a different amount.
 *
 * @param amount The order amount, for example `"55.00"`.
 * @param currencyCode The ISO-4217 currency code for [amount]. If the checkout request's is `null`, pass the merchant
 * account's currency.
 * @param merchantAccountId Optional. A non-default merchant account.
 */
@ExperimentalBetaApi
class PayPalSavedPaymentMethodRequest @JvmOverloads constructor(
    internal val amount: String,
    internal val currencyCode: String,
    internal val merchantAccountId: String? = null
) {

    // Compared by value, like iOS's Equatable, so a request rebuilt with the same values doesn't count as a change.
    override fun equals(other: Any?): Boolean =
        other is PayPalSavedPaymentMethodRequest &&
            amount == other.amount &&
            currencyCode == other.currencyCode &&
            merchantAccountId == other.merchantAccountId

    override fun hashCode(): Int = Objects.hash(amount, currencyCode, merchantAccountId)
}
