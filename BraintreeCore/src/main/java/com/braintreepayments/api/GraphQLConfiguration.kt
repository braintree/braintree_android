package com.braintreepayments.api

import android.text.TextUtils
import org.json.JSONArray
import org.json.JSONObject
import java.util.HashSet

/**
 * Contains the remote GraphQL configuration for the Braintree SDK.
 * @property url the GraphQL url
 * @property isEnabled `true` if GraphQL is enabled, `false` otherwise.
 */
internal data class GraphQLConfiguration(val url: String, private val features: Set<String>) {

    constructor(json: JSONObject?) : this(
        Json.optString(json, GraphQLConstants.Keys.URL, ""),
        parseJsonFeatures(json?.optJSONArray(GraphQLConstants.Keys.FEATURES))
    )

    /**
     * @return `true` if GraphQL is enabled, `false` otherwise.
     */
    val isEnabled: Boolean = !TextUtils.isEmpty(url)

    /**
     * Check if a specific feature is enabled in the GraphQL API.
     *
     * @param feature The feature to check.
     * @return `true` if GraphQL is enabled and the feature is enabled, `false` otherwise.
     */
    fun isFeatureEnabled(feature: String): Boolean {
        return isEnabled && features.contains(feature)
    }

    companion object {
        private fun parseJsonFeatures(jsonArray: JSONArray?): Set<String> {
            val features: MutableSet<String> = HashSet()
            jsonArray?.also { array ->
                for (i in 0 until array.length()) {
                    features.add(array.optString(i, ""))
                }
            }
            return features
        }
    }
}
