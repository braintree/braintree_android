package com.braintreepayments.api;

import androidx.annotation.VisibleForTesting;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;

/**
 * Class that handles parsing http responses for {@link BraintreeGraphQLClient}.
 */
class BraintreeGraphQLResponseParser implements HttpResponseParser {

    private final HttpResponseParser baseParser;

    BraintreeGraphQLResponseParser() {
        this(new BaseHttpResponseParser());
    }

    @VisibleForTesting
    BraintreeGraphQLResponseParser(HttpResponseParser baseParser) {
        this.baseParser = baseParser;
    }

    /**
     * @param responseCode the response code returned when the http request was made.
     * @param connection the connection through which the http request was made.
     * @return the body of the http response.
     */
    @Override
    public String parse(int responseCode, HttpURLConnection connection) throws Exception {
        String response = baseParser.parse(responseCode, connection);
        JSONArray errors = new JSONObject(response)
                .optJSONArray(GraphQLConstants.Keys.ERRORS);

        if (errors != null) {
            for (int i = 0; i < errors.length(); i++) {
                JSONObject error = errors.getJSONObject(i);
                JSONObject extensions = error.optJSONObject(GraphQLConstants.Keys.EXTENSIONS);
                String message = Json.optString(error, GraphQLConstants.Keys.MESSAGE, "An Unexpected Exception Occurred");

                if (extensions == null) {
                    throw new UnexpectedException(message);
                }

                if (Json.optString(extensions, GraphQLConstants.Keys.LEGACY_CODE, "").equals(GraphQLConstants.LegacyErrorCodes.VALIDATION_NOT_ALLOWED)) {
                    throw new AuthorizationException(error.getString(GraphQLConstants.Keys.MESSAGE));
                } else if (!Json.optString(extensions, GraphQLConstants.Keys.ERROR_TYPE, "").equals(GraphQLConstants.ErrorTypes.USER)) {
                    throw new UnexpectedException(message);
                }
            }

            throw ErrorWithResponse.fromGraphQLJson(response);
        }
        return response;
    }
}
