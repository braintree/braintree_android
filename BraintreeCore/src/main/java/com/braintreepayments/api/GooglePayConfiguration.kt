package com.braintreepayments.api

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Contains the remote Google Pay configuration for the Braintree SDK.
 */
internal class GooglePayConfiguration(json: JSONObject?) {

    companion object {
        private const val ENABLED_KEY = "enabled"
        private const val GOOGLE_AUTHORIZATION_FINGERPRINT_KEY = "googleAuthorizationFingerprint"
        private const val ENVIRONMENT_KEY = "environment"
        private const val DISPLAY_NAME_KEY = "displayName"
        private const val SUPPORTED_NETWORKS_KEY = "supportedNetworks"
        private const val PAYPAL_CLIENT_ID_KEY = "paypalClientId"
    }

    /**
     * @return `true` if Google Pay is enabled and supported in the current environment,
     * `false` otherwise.
     */
    val isEnabled: Boolean

    /**
     * @return the authorization fingerprint to use for Google Pay, only allows tokenizing Google Pay cards.
     */
    var googleAuthorizationFingerprint: String?

    /**
     * @return the current Google Pay environment.
     */
    var environment: String?

    /**
     * @return the display name to show to the user.
     */
    val displayName: String?

    /**
     * @return a string array of supported card networks for Google Pay.
     */
    val supportedNetworks: List<String>?

    /**
     * @return the PayPal Client ID.
     */
    val paypalClientId: String?

    init {
        val jsonObject = json ?: JSONObject()
        isEnabled = jsonObject.optBoolean(ENABLED_KEY, false)
        googleAuthorizationFingerprint = Json.optString(
            json,
            GOOGLE_AUTHORIZATION_FINGERPRINT_KEY, null
        )
        environment = Json.optString(jsonObject, ENVIRONMENT_KEY, null)
        displayName = Json.optString(jsonObject, DISPLAY_NAME_KEY, "")
        paypalClientId = Json.optString(jsonObject, PAYPAL_CLIENT_ID_KEY, "")

        supportedNetworks = parseSupportedNetworks(jsonObject.optJSONArray(SUPPORTED_NETWORKS_KEY))
    }

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