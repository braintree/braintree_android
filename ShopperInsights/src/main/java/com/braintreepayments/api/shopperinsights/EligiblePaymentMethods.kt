package com.braintreepayments.api.shopperinsights

import org.json.JSONObject

/**
 * Contains details about the available payment methods.
 *
 * @property paypal Details about the PayPal payment method.
 * @property venmo Details about the Venmo payment method.
 */
internal data class EligiblePaymentMethods(
    val paypal: EligiblePaymentMethodDetails?,
    val venmo: EligiblePaymentMethodDetails?
) {
    companion object {
        fun fromJson(jsonObject: JSONObject): EligiblePaymentMethods {
            val paypal = jsonObject.optJSONObject("paypal")?.let {
                EligiblePaymentMethodDetails.fromJson(it)
            }
            val venmo = jsonObject.optJSONObject("venmo")?.let {
                EligiblePaymentMethodDetails.fromJson(it)
            }
            return EligiblePaymentMethods(paypal, venmo)
        }
    }
}
