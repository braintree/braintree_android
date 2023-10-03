package com.braintreepayments.api

import android.text.TextUtils
import org.json.JSONArray
import org.json.JSONObject

/**
 * Contains the remote Samsung Pay configuration for the Braintree SDK.
 * @property environment the Braintree environment Samsung Pay should interact with.
 * @property serviceId the service id associated with the merchant.
 * @property merchantDisplayName the merchant display name for Samsung Pay.
 * @property supportedCardBrands a list of card brands supported by Samsung Pay.
 * @property samsungAuthorization the authorization to use with Samsung Pay.
 */
internal data class SamsungPayConfiguration(
    val environment: String,
    val serviceId: String,
    val merchantDisplayName: String,
    val supportedCardBrands: List<String>,
    val samsungAuthorization: String
) {

    constructor(json: JSONObject?) : this(
        Json.optString(json, ENVIRONMENT, ""),
        Json.optString(json, SERVICE_ID_KEY, ""),
        Json.optString(json, DISPLAY_NAME_KEY, ""),
        parseSupportedCardBrands(json?.optJSONArray(SUPPORTED_CARD_BRANDS_KEY)),
        Json.optString(json, SAMSUNG_AUTHORIZATION_KEY, "")
    )
    /**
     * @return `true` if Samsung Pay is enabled, `false` otherwise.
     */
    val isEnabled: Boolean = !TextUtils.isEmpty(samsungAuthorization)

    companion object {
        private const val DISPLAY_NAME_KEY = "displayName"
        private const val SERVICE_ID_KEY = "serviceId"
        private const val SUPPORTED_CARD_BRANDS_KEY = "supportedCardBrands"
        private const val SAMSUNG_AUTHORIZATION_KEY = "samsungAuthorization"
        private const val ENVIRONMENT = "environment"

        private fun parseSupportedCardBrands(jsonArray: JSONArray?): List<String> {
            val result = mutableListOf<String>()
            jsonArray?.also { array ->
                for (i in 0 until array.length()) {
                    result.add(array.getString(i))
                }
            }
            return result
        }
    }
}
