package com.braintreepayments.api.shopperinsights

import org.json.JSONObject

/**
 * Represents the result from the find eligible payments api.
 *
 * @property eligibleMethods Contains the payment methods available to the shopper.
 */
internal data class EligiblePaymentsApiResult(
    val eligibleMethods: EligiblePaymentMethods
) {
    companion object {
        fun fromJson(jsonString: String): EligiblePaymentsApiResult {
            val jsonObject = JSONObject(jsonString)
            val eligibleMethodsJson = jsonObject.getJSONObject("eligible_methods")
            return EligiblePaymentsApiResult(EligiblePaymentMethods.fromJson(eligibleMethodsJson))
        }
    }
}
