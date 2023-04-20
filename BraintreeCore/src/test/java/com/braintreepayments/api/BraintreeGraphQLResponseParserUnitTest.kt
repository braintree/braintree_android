package com.braintreepayments.api

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import java.net.HttpURLConnection

class BraintreeGraphQLResponseParserUnitTest {

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
        val responseWithSuccess = Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD
        every { baseParser.parse(123, urlConnection) } returns responseWithSuccess

        val sut = BraintreeGraphQLResponseParser(baseParser)
        val result = sut.parse(123, urlConnection)
        assertEquals(responseWithSuccess, result)
    }

    @Test
    @Throws(Exception::class)
    fun parse_propagatesExceptionsByDefault() {
        exceptionRule.expect(Exception::class.java)
        exceptionRule.expectMessage("error")

        val exception = Exception("error")
        every { baseParser.parse(123, urlConnection) } throws exception

        val sut = BraintreeGraphQLResponseParser(baseParser)
        sut.parse(123, urlConnection)
    }

    @Test
    @Throws(Exception::class)
    fun parse_onUserError_throwsErrorWithResponseException() {
        val responseWithUserError = Fixtures.ERRORS_GRAPHQL_CREDIT_CARD_ERROR
        every { baseParser.parse(123, urlConnection) } returns responseWithUserError

        val sut = BraintreeGraphQLResponseParser(baseParser)
        try {
            sut.parse(123, urlConnection)
            fail("No exception was thrown")
        } catch (e: ErrorWithResponse) {
            assertEquals("Input is invalid.", e.message)
            assertNotNull(e.errorFor("creditCard"))
        }
    }

    @Test
    @Throws(Exception::class)
    fun parse_onValidationNotAllowed_throwsAuthorizationException() {
        val responseWithValidationNotAllowed = Fixtures.ERRORS_GRAPHQL_VALIDATION_NOT_ALLOWED_ERROR
        every { baseParser.parse(123, urlConnection) } returns responseWithValidationNotAllowed

        val sut = BraintreeGraphQLResponseParser(baseParser)
        try {
            sut.parse(123, urlConnection)
            fail("No exception was thrown")
        } catch (e: AuthorizationException) {
            assertEquals(
                "Validation is not supported for requests authorized with a tokenization key.",
                e.message
            )
        }
    }

    @Test
    @Throws(Exception::class)
    fun parse_onCoercionError_throwsUnexpectedException() {
        val responseWithCoercionError = Fixtures.ERRORS_GRAPHQL_COERCION_ERROR
        every { baseParser.parse(123, urlConnection) } returns responseWithCoercionError

        val sut = BraintreeGraphQLResponseParser(baseParser)
        try {
            sut.parse(123, urlConnection)
            fail("No exception was thrown")
        } catch (e: UnexpectedException) {
            assertEquals(
                "Variable 'input' has coerced Null value for NonNull type 'String!'",
                e.message
            )
        }
    }

    @Test
    @Throws(Exception::class)
    fun parse_onUnknownError_throwsUnexpectedException() {
        val responseWithUnknownError = Fixtures.ERRORS_GRAPHQL_UNKNOWN_ERROR
        every { baseParser.parse(123, urlConnection) } returns responseWithUnknownError

        val sut = BraintreeGraphQLResponseParser(baseParser)
        try {
            sut.parse(123, urlConnection)
            fail("No exception was thrown")
        } catch (e: UnexpectedException) {
            assertEquals("An Unexpected Exception Occurred", e.message)
        }
    }
}