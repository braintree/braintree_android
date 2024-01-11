package com.braintreepayments.api

import org.json.JSONObject

/**
 * Details of a specific payment method.
 *
 * @property canBeVaulted Indicates if the payment method can be saved for future transactions.
 * @property eligibleInPaypalNetwork Indicates if the payment method is eligible in the PayPal network.
 * @property recommended Indicates if this payment method is recommended for the shopper.
 * @property recommendedPriority The priority ranking of this payment method if recommended.
 */
internal data class ShopperInsightsPaymentMethodDetails(
    val canBeVaulted: Boolean,
    val eligibleInPaypalNetwork: Boolean,
    val recommended: Boolean,
    val recommendedPriority: Int
) {
    companion object {
        fun fromJson(jsonObject: JSONObject): ShopperInsightsPaymentMethodDetails {
            return ShopperInsightsPaymentMethodDetails(
                canBeVaulted = jsonObject.optBoolean("can_be_vaulted"),
                eligibleInPaypalNetwork = jsonObject.optBoolean("eligible_in_paypal_network"),
                recommended = jsonObject.optBoolean("recommended"),
                recommendedPriority = jsonObject.optInt("recommended_priority")
            )
        }
    }
}
