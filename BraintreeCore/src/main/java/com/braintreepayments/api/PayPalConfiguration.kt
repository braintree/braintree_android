package com.braintreepayments.api

import android.text.TextUtils
import org.json.JSONObject

/**
 * Contains the remote PayPal configuration for the Braintree SDK.
 * @property directBaseUrl the url for custom PayPal environments.
 * @property displayName the PayPal app display name.
 * @property clientId the PayPal app client id.
 * @property privacyUrl the PayPal app privacy url.
 * @property userAgreementUrl the PayPal app user agreement url.
 * @property environment the current environment for PayPal.
 * @property isTouchDisabled `true` if PayPal touch is currently disabled, `false` otherwise.
 * @property currencyIsoCode the PayPal currency code.
 */
internal class PayPalConfiguration constructor(
    val directBaseUrl: String?,
    val displayName: String?,
    val clientId: String?,
    val privacyUrl: String?,
    val userAgreementUrl: String?,
    val environment: String?,
    val isTouchDisabled: Boolean,
    val currencyIsoCode: String?
) {

    constructor(jsonObject: JSONObject?) : this(
        parseBaseUrl(Json.optString(jsonObject, DIRECT_BASE_URL_KEY, null)),
        Json.optString(jsonObject, DISPLAY_NAME_KEY, null),
        Json.optString(jsonObject, CLIENT_ID_KEY, null),
        Json.optString(jsonObject, PRIVACY_URL_KEY, null),
        Json.optString(jsonObject, USER_AGREEMENT_URL_KEY, null),
        Json.optString(jsonObject, ENVIRONMENT_KEY, null),
        jsonObject?.optBoolean(TOUCH_DISABLED_KEY, true) ?: true,
        Json.optString(jsonObject, CURRENCY_ISO_CODE_KEY, null)
    )

    companion object {
        private const val DISPLAY_NAME_KEY = "displayName"
        private const val CLIENT_ID_KEY = "clientId"
        private const val PRIVACY_URL_KEY = "privacyUrl"
        private const val USER_AGREEMENT_URL_KEY = "userAgreementUrl"
        private const val DIRECT_BASE_URL_KEY = "directBaseUrl"
        private const val ENVIRONMENT_KEY = "environment"
        private const val TOUCH_DISABLED_KEY = "touchDisabled"
        private const val CURRENCY_ISO_CODE_KEY = "currencyIsoCode"

        private fun parseBaseUrl(url: String?): String? {
            return if (TextUtils.isEmpty(url)) null else "$url/v1/"
        }
    }
}
