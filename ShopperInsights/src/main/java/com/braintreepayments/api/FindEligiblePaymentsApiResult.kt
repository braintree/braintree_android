package com.braintreepayments.api

import org.json.JSONObject

/**
 * Represents the result from the find eligible payments api.
 *
 * @property eligibleMethods Contains the payment methods available to the shopper.
 */
internal data class FindEligiblePaymentsApiResult(
    val eligibleMethods: FindEligiblePaymentMethods
) {
    companion object {
        fun fromJson(jsonString: String): FindEligiblePaymentsApiResult {
            val jsonObject = JSONObject(jsonString)
            val eligibleMethodsJson = jsonObject.getJSONObject("eligible_methods")
            return FindEligiblePaymentsApiResult(FindEligiblePaymentMethods.fromJson(eligibleMethodsJson))
        }
    }
}
