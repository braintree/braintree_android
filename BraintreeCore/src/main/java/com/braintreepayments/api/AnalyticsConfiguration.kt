package com.braintreepayments.api

import android.text.TextUtils
import org.json.JSONObject

/**
 * Contains configuration for Braintree analytics calls
 * @property url url of the Braintree analytics service.
 */
internal data class AnalyticsConfiguration(val url: String) {

    constructor(json: JSONObject?) : this(
        Json.optString(json, URL_KEY, "")
    )

    /**
     * @return `true` if analytics are enabled, `false` otherwise.
     */
    val isEnabled: Boolean = !TextUtils.isEmpty(url)

    companion object {
        private const val URL_KEY = "url"
    }
}
