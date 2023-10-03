package com.braintreepayments.api

import androidx.annotation.VisibleForTesting
import org.json.JSONObject
import java.net.HttpURLConnection

/**
 * Class that handles parsing http responses for [BraintreeGraphQLClient].
 */
internal class BraintreeGraphQLResponseParser @VisibleForTesting constructor(
    private val baseParser: HttpResponseParser
) : HttpResponseParser {

    constructor() : this(BaseHttpResponseParser())

    /**
     * @param responseCode the response code returned when the http request was made.
     * @param connection the connection through which the http request was made.
     * @return the body of the http response.
     */
    @Throws(Exception::class)
    override fun parse(responseCode: Int, connection: HttpURLConnection): String {
        val response = baseParser.parse(responseCode, connection)
        val errors = JSONObject(response).optJSONArray(GraphQLConstants.Keys.ERRORS)
        if (errors == null) return response

        for (i in 0 until errors.length()) {
            val error = errors.getJSONObject(i)
            val extensions = error.optJSONObject(GraphQLConstants.Keys.EXTENSIONS)
            val message = Json.optString(
                error,
                GraphQLConstants.Keys.MESSAGE,
                "An Unexpected Exception Occurred"
            )
            if (extensions == null) {
                throw UnexpectedException(message)
            }

            val legacyCode =
                Json.optString(extensions, GraphQLConstants.Keys.LEGACY_CODE, "")
            val errorType =
                Json.optString(extensions, GraphQLConstants.Keys.ERROR_TYPE, "")

            if (legacyCode == GraphQLConstants.LegacyErrorCodes.VALIDATION_NOT_ALLOWED) {
                throw AuthorizationException(error.getString(GraphQLConstants.Keys.MESSAGE))
            } else if (errorType != GraphQLConstants.ErrorTypes.USER) {
                throw UnexpectedException(message)
            }
        }
        throw ErrorWithResponse.fromGraphQLJson(response)
    }
}
