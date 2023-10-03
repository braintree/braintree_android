package com.braintreepayments.api

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Contains the remote Google Pay configuration for the Braintree SDK.
 * @property isEnabled {@code true} if Google Pay is enabled and supported in the current
 * environment, {@code false} otherwise.
 * @property googleAuthorizationFingerprint the authorization fingerprint to use for Google Pay,
 * only allows tokenizing Google Pay cards.
 * @property environment the current Google Pay environment.
 * @property displayName the display name to show to the user.
 * @property supportedNetworks a string array of supported card networks for Google Pay.
 * @property paypalClientId the PayPal Client ID.
 */
internal data class GooglePayConfiguration constructor(
    val isEnabled: Boolean,
    val googleAuthorizationFingerprint: String?,
    val environment: String?,
    val displayName: String,
    val supportedNetworks: List<String>,
    val paypalClientId: String
) {

    constructor(json: JSONObject?) : this(
        json?.optBoolean(ENABLED_KEY, false) ?: false,
        Json.optString(json, GOOGLE_AUTHORIZATION_FINGERPRINT_KEY, null),
        Json.optString(json, ENVIRONMENT_KEY, null),
        Json.optString(json, DISPLAY_NAME_KEY, ""),
        parseSupportedNetworks(json?.optJSONArray(SUPPORTED_NETWORKS_KEY)),
        Json.optString(json, PAYPAL_CLIENT_ID_KEY, ""),
    )

    companion object {
        private const val ENABLED_KEY = "enabled"
        private const val GOOGLE_AUTHORIZATION_FINGERPRINT_KEY = "googleAuthorizationFingerprint"
        private const val ENVIRONMENT_KEY = "environment"
        private const val DISPLAY_NAME_KEY = "displayName"
        private const val SUPPORTED_NETWORKS_KEY = "supportedNetworks"
        private const val PAYPAL_CLIENT_ID_KEY = "paypalClientId"

        private fun parseSupportedNetworks(networksArray: JSONArray?): List<String> {
            val result: MutableList<String> = mutableListOf()
            networksArray?.also { array ->
                for (i in 0 until array.length()) {
                    try {
                        result.add(array.getString(i))
                    } catch (ignored: JSONException) {
                    }
                }
            }
            return result
        }
    }
}
