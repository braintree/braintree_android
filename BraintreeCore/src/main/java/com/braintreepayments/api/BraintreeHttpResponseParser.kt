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
    @Suppress("SwallowedException")
    override fun parse(responseCode: Int, connection: HttpURLConnection): String = try {
        baseParser.parse(responseCode, connection)
    } catch (e: AuthorizationException) {
        val errorMessage = ErrorWithResponse(AUTH_ERROR_CODE, e.message).message
        throw AuthorizationException(errorMessage)
    } catch (e: UnprocessableEntityException) {
        throw ErrorWithResponse(UNPROCESSABLE_ENTITY_ERROR_CODE, e.message)
    }

    companion object {
        private const val AUTH_ERROR_CODE = 403
        private const val UNPROCESSABLE_ENTITY_ERROR_CODE = 422
    }
}
