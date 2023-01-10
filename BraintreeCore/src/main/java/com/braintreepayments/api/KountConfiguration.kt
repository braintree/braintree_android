package com.braintreepayments.api

import android.text.TextUtils
import org.json.JSONObject

/**
 * Contains the remote Kount configuration for the Braintree SDK.
 * @property kountMerchantId the Kount Merchant Id.
 * @property isEnabled `true` if Kount is enabled, `false` otherwise.
 */
internal data class KountConfiguration private constructor(
    val kountMerchantId: String? = null,
    val isEnabled: Boolean = !TextUtils.isEmpty(kountMerchantId)
) {

    constructor(json: JSONObject?) : this(Json.optString(json, KOUNT_MERCHANT_ID_KEY, ""))

    companion object {
        private const val KOUNT_MERCHANT_ID_KEY = "kountMerchantId"
    }
}