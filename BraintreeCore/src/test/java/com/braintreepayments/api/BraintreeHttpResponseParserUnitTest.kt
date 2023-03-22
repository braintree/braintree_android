package com.braintreepayments.api

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import java.net.HttpURLConnection

class BraintreeHttpResponseParserUnitTest {

    private lateinit var urlConnection: HttpURLConnection
    private lateinit var baseParser: BaseHttpResponseParser

    @Rule
    @JvmField
    var exceptionRule: ExpectedException = ExpectedException.none()

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
        exceptionRule.expect(Exception::class.java)
        exceptionRule.expectMessage("error")

        val exception = Exception("error")
        every { baseParser.parse(123, urlConnection) } throws exception

        val sut = BraintreeHttpResponseParser(baseParser)
        sut.parse(123, urlConnection)
    }

    @Test
    @Throws(Exception::class)
    fun parse_whenBaseParserThrowsAuthorizationException_throwsNewAuthorizationExceptionWithMessage() {
        exceptionRule.expect(AuthorizationException::class.java)
        exceptionRule.expectMessage("There was an error")

        val authorizationException = AuthorizationException(Fixtures.ERROR_RESPONSE)
        every { baseParser.parse(123, urlConnection) } throws authorizationException

        val sut = BraintreeHttpResponseParser(baseParser)
        sut.parse(123, urlConnection)
    }

    @Test
    @Throws(Exception::class)
    fun parse_whenBaseParserThrowsUnprocessibleEntityException_throwsErrorWithResponseException() {
        exceptionRule.expect(ErrorWithResponse::class.java)
        exceptionRule.expectMessage("There was an error")
        val unprocessableEntityException = UnprocessableEntityException(Fixtures.ERROR_RESPONSE)

        every {
            baseParser.parse(123, urlConnection)
        } throws unprocessableEntityException

        val sut = BraintreeHttpResponseParser(baseParser)
        sut.parse(123, urlConnection)
    }
}