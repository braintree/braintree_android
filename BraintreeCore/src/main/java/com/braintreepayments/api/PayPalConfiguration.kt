package com.braintreepayments.api

import android.text.TextUtils
import org.json.JSONObject

/**
 * Contains the remote PayPal configuration for the Braintree SDK.
 */
internal class PayPalConfiguration(json: JSONObject?) {

    companion object {
        private const val DISPLAY_NAME_KEY = "displayName"
        private const val CLIENT_ID_KEY = "clientId"
        private const val PRIVACY_URL_KEY = "privacyUrl"
        private const val USER_AGREEMENT_URL_KEY = "userAgreementUrl"
        private const val DIRECT_BASE_URL_KEY = "directBaseUrl"
        private const val ENVIRONMENT_KEY = "environment"
        private const val TOUCH_DISABLED_KEY = "touchDisabled"
        private const val CURRENCY_ISO_CODE_KEY = "currencyIsoCode"
    }

    /**
     * @return the url for custom PayPal environments.
     */
    val directBaseUrl: String?
        get() = if (TextUtils.isEmpty(field)) null else "$field/v1/"

    /**
     * @return the PayPal app display name.
     */
    val displayName: String?

    /**
     * @return the PayPal app client id.
     */
    val clientId: String?

    /**
     * @return the PayPal app privacy url.
     */
    var privacyUrl: String?

    /**
     * @return the PayPal app user agreement url.
     */
    var userAgreementUrl: String?

    /**
     * @return the current environment for PayPal.
     */
    var environment: String?

    /**
     * @return `true` if PayPal touch is currently disabled, `false` otherwise.
     */
    var isTouchDisabled = false

    /**
     * @return the PayPal currency code.
     */
    var currencyIsoCode: String?

    init {
        val jsonObject = json ?: JSONObject()
        displayName = Json.optString(jsonObject, DISPLAY_NAME_KEY, null)
        clientId = Json.optString(jsonObject, CLIENT_ID_KEY, null)
        privacyUrl = Json.optString(jsonObject, PRIVACY_URL_KEY, null)
        userAgreementUrl =
            Json.optString(jsonObject, USER_AGREEMENT_URL_KEY, null)
        directBaseUrl = Json.optString(jsonObject, DIRECT_BASE_URL_KEY, null)
        environment = Json.optString(jsonObject, ENVIRONMENT_KEY, null)
        isTouchDisabled = jsonObject.optBoolean(TOUCH_DISABLED_KEY, true)
        currencyIsoCode = Json.optString(jsonObject, CURRENCY_ISO_CODE_KEY, null)
    }
}