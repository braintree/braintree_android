package com.braintreepayments.api

import org.json.JSONObject

/**
 * Contains details about the available payment methods.
 *
 * @property paypal Details about the PayPal payment method.
 */
internal data class EligiblePaymentMethods(
    val paypal: EligiblePaymentMethodDetails?
) {
    companion object {
        fun fromJson(jsonObject: JSONObject): EligiblePaymentMethods {
            val paypal = jsonObject.optJSONObject("paypal")?.let {
                EligiblePaymentMethodDetails.fromJson(it)
            }
            return EligiblePaymentMethods(paypal)
        }
    }
}
