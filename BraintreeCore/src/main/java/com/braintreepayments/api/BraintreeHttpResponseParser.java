package com.braintreepayments.api;

import androidx.annotation.VisibleForTesting;

import java.net.HttpURLConnection;

/**
 * Class that handles parsing http responses for {@link BraintreeHttpClient}.
 */
class BraintreeHttpResponseParser implements HttpResponseParser {

    private final HttpResponseParser baseParser;

    BraintreeHttpResponseParser() {
        this(new BaseHttpResponseParser());
    }

    @VisibleForTesting
    BraintreeHttpResponseParser(HttpResponseParser baseParser) {
        this.baseParser = baseParser;
    }

    /**
     * @param responseCode the response code returned when the http request was made.
     * @param connection the connection through which the http request was made.
     * @return the body of the http response.
     */
    @Override
    public String parse(int responseCode, HttpURLConnection connection) throws Exception {
        try {
            return baseParser.parse(responseCode, connection);
        } catch (AuthorizationException | UnprocessableEntityException e) {
            if (e instanceof AuthorizationException) {
                String errorMessage = new ErrorWithResponse(403, e.getMessage()).getMessage();
                throw new AuthorizationException(errorMessage);
            } else {
                throw new ErrorWithResponse(422, e.getMessage());
            }
        }
    }
}
