package com.braintreepayments.api

import org.json.JSONObject

/**
 * Contains details about the available payment methods.
 *
 * @property paypal Details about the PayPal payment method.
 * @property venmo Details about the Venmo payment method.
 */
internal data class ShopperInsightsPaymentMethods(
    val paypal: ShopperInsightsPaymentMethodDetails?,
    val venmo: ShopperInsightsPaymentMethodDetails?
) {
    companion object {
        fun fromJson(jsonObject: JSONObject): ShopperInsightsPaymentMethods {
            val paypal = jsonObject.optJSONObject("paypal")?.let {
                ShopperInsightsPaymentMethodDetails.fromJson(it)
            }
            val venmo = jsonObject.optJSONObject("venmo")?.let {
                ShopperInsightsPaymentMethodDetails.fromJson(it)
            }
            return ShopperInsightsPaymentMethods(paypal, venmo)
        }
    }
}
