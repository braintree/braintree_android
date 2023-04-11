package com.braintreepayments.api

import android.text.TextUtils
import org.json.JSONObject

/**
 * Contains the remote Kount configuration for the Braintree SDK.
 * @property kountMerchantId the Kount Merchant Id.
 * @property isEnabled `true` if Kount is enabled, `false` otherwise.
 */
internal data class KountConfiguration internal constructor(val kountMerchantId: String) {

    constructor(json: JSONObject?) : this(Json.optString(json, KOUNT_MERCHANT_ID_KEY, ""))

    val isEnabled: Boolean = !TextUtils.isEmpty(kountMerchantId)

    companion object {
        private const val KOUNT_MERCHANT_ID_KEY = "kountMerchantId"
    }
}