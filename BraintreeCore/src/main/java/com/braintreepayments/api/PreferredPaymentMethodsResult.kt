package com.braintreepayments.api

import androidx.annotation.RestrictTo
import org.json.JSONException
import org.json.JSONObject

//NEXT MAJOR VERSION: constructor with optional params
/**
 * Contains information about which payment methods are preferred on the device.
 * This class is currently in beta and may be removed in future releases.
 * @hide
 */
open class PreferredPaymentMethodsResult @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) constructor() {

    private var isPayPalPreferred = false
    private var isVenmoPreferred = false

    /**
     *
     * @return True if PayPal is a preferred payment method. False otherwise.
     */
    open fun isPayPalPreferred() = isPayPalPreferred

    /**
     *
     * @return True if Venmo app is installed. False otherwise.
     */
    open fun isVenmoPreferred() = isVenmoPreferred

    open fun isPayPalPreferred(payPalPreferred: Boolean): PreferredPaymentMethodsResult {
        isPayPalPreferred = payPalPreferred
        return this
    }

    open fun isVenmoPreferred(venmoPreferred: Boolean): PreferredPaymentMethodsResult {
        isVenmoPreferred = venmoPreferred
        return this
    }

    companion object {
        @JvmStatic
        fun fromJSON(
            responseBody: String,
            venmoInstalled: Boolean
        ): PreferredPaymentMethodsResult {
            var payPalPreferred = false
            try {
                val response = JSONObject(responseBody)
                val preferredPaymentMethods =
                    getObjectAtKeyPath(response, "data.preferredPaymentMethods")
                if (preferredPaymentMethods != null) { //NEXT MAJOR VERSION: this shouldn't be null
                    payPalPreferred = preferredPaymentMethods.getBoolean("paypalPreferred")
                }
            } catch (ignored: JSONException) {
                // do nothing
                print(ignored.message)
            }
            return PreferredPaymentMethodsResult()
                .isPayPalPreferred(payPalPreferred)
                .isVenmoPreferred(venmoInstalled)
        }

        @Throws(JSONException::class)
        private fun getObjectAtKeyPath(obj: JSONObject, keyPath: String): JSONObject {
            val keys = keyPath.split(".").toTypedArray()
            var result = obj
            for (key in keys) {
                result = result.getJSONObject(key)
            }
            return result
        }
    }
}