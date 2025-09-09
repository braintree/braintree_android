package com.braintreepayments.api.shopperinsights.v2.internal

import org.json.JSONException
import org.json.JSONObject

/**
 * Parser for Shopper Insights v2 API responses.
 */
internal class ShopperInsightsResponseParser {

    @Throws(JSONException::class)
    fun parseSessionId(responseBody: String, graphQLCall: String): String {
        val data = JSONObject(responseBody).getJSONObject(DATA)
        val sessionObject = data.getJSONObject(graphQLCall)
        return sessionObject.getString(SESSION_ID)
    }

    companion object {
        private const val DATA = "data"
        private const val SESSION_ID = "sessionId"
    }
}
