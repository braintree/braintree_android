package com.braintreepayments.api.shopperinsights.v2.internal

import com.braintreepayments.api.core.BraintreeException
import com.braintreepayments.api.core.GraphQLConstants
import org.json.JSONException
import org.json.JSONObject

/**
 * Parser for Shopper Insights v2 API responses.
 */
internal class ShopperInsightsResponseParser {

    @Throws(JSONException::class, BraintreeException::class)
    fun parseSessionId(responseBody: String, graphQLCall: String): String {
        val responseJSON = JSONObject(responseBody)

        val errors = responseJSON.optJSONArray(GraphQLConstants.Keys.ERRORS)
        if (errors != null && errors.length() > 0) {
            val message = errors.getJSONObject(0).optString(GraphQLConstants.Keys.MESSAGE, responseBody)
            throw BraintreeException(message)
        }

        val data = responseJSON.getJSONObject(DATA)
        val sessionObject = data.getJSONObject(graphQLCall)
        return sessionObject.getString(SESSION_ID)
    }

    companion object {
        private const val DATA = "data"
        private const val SESSION_ID = "sessionId"
    }
}
