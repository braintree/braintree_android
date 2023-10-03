package com.braintreepayments.api

import android.text.TextUtils
import org.json.JSONObject

/**
 * Contains the remote Braintree API Configuration for the Braintree SDK.
 * @property accessToken The Access Token for Braintree API.
 * @property url the base url for accessing Braintree API.
 */
internal data class BraintreeApiConfiguration(val accessToken: String, val url: String) {

    constructor(json: JSONObject?) : this(
        Json.optString(json, ACCESS_TOKEN_KEY, ""),
        Json.optString(json, URL_KEY, "")
    )

    /**
     * @return a boolean indicating whether Braintree API is enabled for this merchant.
     */
    val isEnabled: Boolean = !TextUtils.isEmpty(accessToken)

    companion object {
        private const val ACCESS_TOKEN_KEY = "accessToken"
        private const val URL_KEY = "url"
    }
}
