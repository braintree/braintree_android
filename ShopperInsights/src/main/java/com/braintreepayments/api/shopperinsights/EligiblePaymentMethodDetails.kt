package com.braintreepayments.api.shopperinsights

import org.json.JSONObject

/**
 * Details of a specific payment method.
 *
 * @property canBeVaulted Indicates if the payment method can be saved for future transactions.
 * @property eligibleInPayPalNetwork Indicates if the payment method is eligible in the PayPal network.
 * @property recommended Indicates if this payment method is recommended for the shopper.
 * @property recommendedPriority The priority ranking of this payment method if recommended.
 */
internal data class EligiblePaymentMethodDetails(
    val canBeVaulted: Boolean,
    val eligibleInPayPalNetwork: Boolean,
    val recommended: Boolean,
    val recommendedPriority: Int
) {
    companion object {
        fun fromJson(jsonObject: JSONObject): EligiblePaymentMethodDetails {
            return EligiblePaymentMethodDetails(
                canBeVaulted = jsonObject.optBoolean("can_be_vaulted"),
                eligibleInPayPalNetwork = jsonObject.optBoolean("eligible_in_paypal_network"),
                recommended = jsonObject.optBoolean("recommended"),
                recommendedPriority = jsonObject.optInt("recommended_priority")
            )
        }
    }
}
