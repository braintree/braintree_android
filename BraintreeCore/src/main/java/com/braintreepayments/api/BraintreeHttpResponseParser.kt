package com.braintreepayments.api

import java.net.HttpURLConnection

/**
 * Class that handles parsing http responses for [BraintreeHttpClient].
 */
internal class BraintreeHttpResponseParser(
    private val baseParser: HttpResponseParser = BaseHttpResponseParser()
) : HttpResponseParser {

    /**
     * @param responseCode the response code returned when the http request was made.
     * @param connection the connection through which the http request was made.
     * @return the body of the http response.
     */
    @Throws(Exception::class)
    override fun parse(responseCode: Int, connection: HttpURLConnection): String = try {
        baseParser.parse(responseCode, connection)
    } catch (e: AuthorizationException) {
        val errorMessage = ErrorWithResponse(403, e.message).message
        throw AuthorizationException(errorMessage)
    } catch (e: UnprocessableEntityException) {
        throw ErrorWithResponse(422, e.message)
    }
}