package com.braintreepayments.api

import org.json.JSONArray
import org.json.JSONObject

/**
 * Contains the remote card configuration for the Braintree SDK.
 * @property supportedCardTypes a list of card types supported by the merchant.
 * @property isFraudDataCollectionEnabled if fraud data collection should occur.
 */
internal data class CardConfiguration(
    val supportedCardTypes: List<String>,
    val isFraudDataCollectionEnabled: Boolean
) {

    constructor(json: JSONObject?) : this(
        parseSupportedCardTypes(json?.optJSONArray(SUPPORTED_CARD_TYPES_KEY)),
        json?.optBoolean(COLLECT_DEVICE_DATA_KEY, false) ?: false
    )

    companion object {
        private const val SUPPORTED_CARD_TYPES_KEY = "supportedCardTypes"
        private const val COLLECT_DEVICE_DATA_KEY = "collectDeviceData"

        private fun parseSupportedCardTypes(jsonArray: JSONArray?): List<String> {
            val result = mutableListOf<String>()
            jsonArray?.also { array ->
                for (i in 0 until array.length()) {
                    result.add(array.optString(i, ""))
                }
            }
            return result
        }
    }
}
