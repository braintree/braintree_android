package com.braintreepayments.api

import android.text.TextUtils
import org.json.JSONObject

/**
 * Contains the remote Venmo configuration for the Braintree SDK.
 * @property accessToken The access token to use with Venmo.
 * @property environment The Venmo environment the merchant is running in.
 * @property merchantId The merchant Id associated with this merchant's Venmo integration.
 */
internal class VenmoConfiguration constructor(
    val accessToken: String?,
    val environment: String?,
    val merchantId: String?
) {

    constructor(json: JSONObject?) : this(
        Json.optString(json, ACCESS_TOKEN_KEY, ""),
        Json.optString(json, ENVIRONMENT_KEY, ""),
        Json.optString(json, MERCHANT_ID_KEY, ""),
    )

    val isAccessTokenValid: Boolean = !TextUtils.isEmpty(accessToken)

    companion object {
        private const val ACCESS_TOKEN_KEY = "accessToken"
        private const val ENVIRONMENT_KEY = "environment"
        private const val MERCHANT_ID_KEY = "merchantId"
    }
}