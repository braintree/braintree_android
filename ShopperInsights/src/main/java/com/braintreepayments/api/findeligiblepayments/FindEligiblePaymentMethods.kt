package com.braintreepayments.api.findeligiblepayments

import org.json.JSONObject

/**
 * Contains details about the available payment methods.
 *
 * @property paypal Details about the PayPal payment method.
 * @property venmo Details about the Venmo payment method.
 */
internal data class FindEligiblePaymentMethods(
    val paypal: FindEligiblePaymentMethodDetails?,
    val venmo: FindEligiblePaymentMethodDetails?
) {
    companion object {
        fun fromJson(jsonObject: JSONObject): FindEligiblePaymentMethods {
            val paypal = jsonObject.optJSONObject("paypal")?.let {
                FindEligiblePaymentMethodDetails.fromJson(it)
            }
            val venmo = jsonObject.optJSONObject("venmo")?.let {
                FindEligiblePaymentMethodDetails.fromJson(it)
            }
            return FindEligiblePaymentMethods(paypal, venmo)
        }
    }
}
