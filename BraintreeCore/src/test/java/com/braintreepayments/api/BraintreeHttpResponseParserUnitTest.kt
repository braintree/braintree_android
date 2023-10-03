package com.braintreepayments.api

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.net.HttpURLConnection

class BraintreeHttpResponseParserUnitTest {

    private lateinit var urlConnection: HttpURLConnection
    private lateinit var baseParser: BaseHttpResponseParser

    @Before
    fun beforeEach() {
        baseParser = mockk()
        urlConnection = mockk()
    }

    @Test
    @Throws(Exception::class)
    fun parse_forwardsResultByDefault() {
        every { baseParser.parse(123, urlConnection) } returns "parse result"
        val sut = BraintreeHttpResponseParser(baseParser)
        val result = sut.parse(123, urlConnection)
        assertEquals("parse result", result)
    }

    @Test
    @Throws(Exception::class)
    fun parse_propagatesExceptionsByDefault() {
        val exception = Exception("error")
        every { baseParser.parse(123, urlConnection) } throws exception

        val sut = BraintreeHttpResponseParser(baseParser)
        try {
            sut.parse(123, urlConnection)
            fail("should not get here")
        } catch (actualException: Exception) {
            assertSame(exception, actualException)
        }
    }

    @Test
    @Throws(Exception::class)
    fun parse_whenBaseParserThrowsAuthorizationException_throwsNewAuthorizationExceptionWithMessage() {
        val authorizationException = AuthorizationException(Fixtures.ERROR_RESPONSE)
        every { baseParser.parse(123, urlConnection) } throws authorizationException

        val sut = BraintreeHttpResponseParser(baseParser)
        try {
            sut.parse(123, urlConnection)
            fail("should not get here")
        } catch (actualException: AuthorizationException) {
            assertEquals("There was an error", actualException.message)
        }
    }

    @Test
    @Throws(Exception::class)
    fun parse_whenBaseParserThrowsUnprocessibleEntityException_throwsErrorWithResponseException() {
        val unprocessableEntityException = UnprocessableEntityException(Fixtures.ERROR_RESPONSE)

        every {
            baseParser.parse(123, urlConnection)
        } throws unprocessableEntityException

        val sut = BraintreeHttpResponseParser(baseParser)
        try {
            sut.parse(123, urlConnection)
            fail("should not get here")
        } catch (actualException: ErrorWithResponse) {
            assertEquals("There was an error", actualException.message)
        }
    }
}
