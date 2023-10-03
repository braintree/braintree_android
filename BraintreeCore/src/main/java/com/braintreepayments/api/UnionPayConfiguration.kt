package com.braintreepayments.api

import org.json.JSONObject

/**
 * Determines if UnionPay is available to be used
 * @property isEnabled boolean if UnionPay is enabled, and available to be used
 */
internal data class UnionPayConfiguration(val isEnabled: Boolean) {

    constructor(json: JSONObject?) : this(json?.optBoolean(ENABLED, false) ?: false)

    companion object {
        private const val ENABLED = "enabled"
    }
}
