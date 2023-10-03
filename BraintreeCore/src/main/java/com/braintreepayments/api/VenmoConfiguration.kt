package com.braintreepayments.api

import android.text.TextUtils
import org.json.JSONObject

/**
 * Contains the remote Venmo configuration for the Braintree SDK.
 * @property accessToken The access token to use with Venmo.
 * @property environment The Venmo environment the merchant is running in.
 * @property merchantId The merchant Id associated with this merchant's Venmo integration.
 * @property enrichedCustomerDataEnabled The boolean value indicating whether ECD is enabled for the merchant.
 */
internal class VenmoConfiguration constructor(
    val accessToken: String,
    val environment: String,
    val merchantId: String,
    val enrichedCustomerDataEnabled: Boolean
) {

    constructor(json: JSONObject?) : this(
        Json.optString(json, ACCESS_TOKEN_KEY, ""),
        Json.optString(json, ENVIRONMENT_KEY, ""),
        Json.optString(json, MERCHANT_ID_KEY, ""),
        Json.optBoolean(json, ECD_ENABLED_KEY, false),
    )

    val isAccessTokenValid: Boolean = !TextUtils.isEmpty(accessToken)

    companion object {
        private const val ACCESS_TOKEN_KEY = "accessToken"
        private const val ENVIRONMENT_KEY = "environment"
        private const val MERCHANT_ID_KEY = "merchantId"
        private const val ECD_ENABLED_KEY = "enrichedCustomerDataEnabled"
    }
}
