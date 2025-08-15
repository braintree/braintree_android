package com.braintreepayments.api.sharedutils

import androidx.annotation.RestrictTo
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
object Json {

    /**
     * Returns the value mapped by `name` from the given `json` object as a [String], or the provided nullable
     * `fallback`.
     *
     * If the `json` object is null, the `name` is null, or the value mapped by `name` is null, the provided nullable
     * `fallback` value is returned.
     * This works around the issue where `JSONObject.optString` returns the string "null" for null values.
     *
     * @param json The [JSONObject] to query, or null.
     * @param name The key to look up in the [JSONObject], or null.
     * @param fallback The nullable fallback value to return if the mapping does not exist or is null.
     * @return The mapped [String] value, or the nullable `fallback` if not found.
     */
    fun optString(json: JSONObject?, name: String?, fallback: String?): String? {
        return if (json == null || name == null || json.isNull(name)) {
            fallback
        } else {
            if (fallback != null) {
                json.optString(name, fallback)
            } else {
                val result = json.optString(name)
                if (result.isEmpty() && json.isNull(name)) null else result
            }
        }
    }

    /**
     * Returns the value mapped by `name` from the given `json` object as a non-null [String].
     *
     * If the `json` object is null, the `name` is null, or the value mapped by `name` is null,
     * the provided non-null `fallback` value is returned.
     *
     * @param json The [JSONObject] to query, or null.
     * @param name The key to look up in the [JSONObject], or null.
     * @param fallback The non-null fallback value to return if the mapping does not exist or is null.
     * @return The mapped [String] value, or the non-null `fallback` if not found.
     */
    @JvmName("optStringNonNull")
    fun optString(json: JSONObject?, name: String?, fallback: String): String {
        return if (json == null || name == null || json.isNull(name)) {
            fallback
        } else {
            json.optString(name, fallback)
        }
    }

    /**
     * Returns the value mapped by `name` from the given `json` object as a [Boolean], or the provided fallback value.
     *
     * If the `json` object is null, the `name` is null, or the value mapped by `name` is null, the provided fallback
     * value is returned.
     *
     * @param json The [JSONObject] to query, or null.
     * @param name The key to look up in the [JSONObject], or null.
     * @param fallback The fallback value to return if the mapping does not exist or is null.
     * @return The mapped [Boolean] value, or the fallback if not found.
     */
    fun optBoolean(json: JSONObject?, name: String?, fallback: Boolean): Boolean {
        return if (json == null || name == null || json.isNull(name)) {
            fallback
        } else {
            json.optBoolean(name, fallback)
        }
    }
}
