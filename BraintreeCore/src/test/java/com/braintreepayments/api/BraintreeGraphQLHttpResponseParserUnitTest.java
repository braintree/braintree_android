package com.braintreepayments.api;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.HttpURLConnection;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BraintreeGraphQLHttpResponseParserUnitTest {

    private HttpURLConnection urlConnection;
    private BaseHttpResponseParser baseParser;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void beforeEach() {
        baseParser = mock(BaseHttpResponseParser.class);
        urlConnection = mock(HttpURLConnection.class);
    }

    @Test
    public void parse_forwardsResultByDefault() throws Exception {
        String responseWithSuccess = Fixtures.GRAPHQL_RESPONSE_CREDIT_CARD;
        when(baseParser.parse(123, urlConnection)).thenReturn(responseWithSuccess);

        BraintreeGraphQLHttpResponseParser sut = new BraintreeGraphQLHttpResponseParser(baseParser);
        String result = sut.parse(123, urlConnection);
        assertEquals(responseWithSuccess, result);
    }

    @Test
    public void parse_propagatesExceptionsByDefault() throws Exception {
        exceptionRule.expect(Exception.class);
        exceptionRule.expectMessage("error");

        Exception exception = new Exception("error");
        when(baseParser.parse(123, urlConnection)).thenThrow(exception);

        BraintreeGraphQLHttpResponseParser sut = new BraintreeGraphQLHttpResponseParser(baseParser);
        sut.parse(123, urlConnection);
    }

    @Test
    public void parse_onUserError_throwsErrorWithResponseException() throws Exception {
        String responseWithUserError = Fixtures.ERRORS_GRAPHQL_CREDIT_CARD_ERROR;
        when(baseParser.parse(123, urlConnection)).thenReturn(responseWithUserError);

        BraintreeGraphQLHttpResponseParser sut = new BraintreeGraphQLHttpResponseParser(baseParser);
        try {
            sut.parse(123, urlConnection);
            fail("No exception was thrown");
        } catch (ErrorWithResponse e) {
            assertEquals("Input is invalid.", e.getMessage());
            assertNotNull(e.errorFor("creditCard"));
        }
    }

    @Test
    public void parse_onValidationNotAllowed_throwsAuthorizationException() throws Exception {
        String responseWithValidationNotAllowed = Fixtures.ERRORS_GRAPHQL_VALIDATION_NOT_ALLOWED_ERROR;
        when(baseParser.parse(123, urlConnection)).thenReturn(responseWithValidationNotAllowed);

        BraintreeGraphQLHttpResponseParser sut = new BraintreeGraphQLHttpResponseParser(baseParser);
        try {
            sut.parse(123, urlConnection);
            fail("No exception was thrown");
        } catch (AuthorizationException e) {
            assertEquals("Validation is not supported for requests authorized with a tokenization key.",
                    e.getMessage());
        }
    }

    @Test
    public void parse_onCoercionError_throwsUnexpectedException() throws Exception {
        String responseWithCoercionError = Fixtures.ERRORS_GRAPHQL_COERCION_ERROR;
        when(baseParser.parse(123, urlConnection)).thenReturn(responseWithCoercionError);

        BraintreeGraphQLHttpResponseParser sut = new BraintreeGraphQLHttpResponseParser(baseParser);
        try {
            sut.parse(123, urlConnection);
            fail("No exception was thrown");
        } catch (UnexpectedException e) {
            assertEquals("Variable 'input' has coerced Null value for NonNull type 'String!'", e.getMessage());
        }
    }

    @Test
    public void parse_onUnknownError_throwsUnexpectedException() throws Exception {
        String responseWithUnknownError = Fixtures.ERRORS_GRAPHQL_UNKNOWN_ERROR;
        when(baseParser.parse(123, urlConnection)).thenReturn(responseWithUnknownError);

        BraintreeGraphQLHttpResponseParser sut = new BraintreeGraphQLHttpResponseParser(baseParser);
        try {
            sut.parse(123, urlConnection);
            fail("No exception was thrown");
        } catch (UnexpectedException e) {
            assertEquals("An Unexpected Exception Occurred", e.getMessage());
        }
    }
}