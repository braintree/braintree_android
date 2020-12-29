package com.braintreepayments.api.internal;

import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.UnprocessableEntityException;
import com.braintreepayments.testutils.Fixtures;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.net.HttpURLConnection;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BraintreeHttpResponseParserTest {

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
        when(baseParser.parse(123, urlConnection)).thenReturn("parse result");

        BraintreeHttpResponseParser sut = new BraintreeHttpResponseParser(baseParser);
        String result = sut.parse(123, urlConnection);
        assertEquals("parse result", result);
    }

    @Test
    public void parse_propagatesExceptionsByDefault() throws Exception {
        exceptionRule.expect(Exception.class);
        exceptionRule.expectMessage("error");

        Exception exception = new Exception("error");
        when(baseParser.parse(123, urlConnection)).thenThrow(exception);

        BraintreeHttpResponseParser sut = new BraintreeHttpResponseParser(baseParser);
        sut.parse(123, urlConnection);
    }

    @Test
    public void parse_whenBaseParserThrowsAuthorizationException_throwsNewAuthorizationExceptionWithMessage() throws Exception {
        exceptionRule.expect(AuthorizationException.class);
        exceptionRule.expectMessage("There was an error");

        AuthorizationException authorizationException = new AuthorizationException(Fixtures.ERROR_RESPONSE);
        when(baseParser.parse(123, urlConnection)).thenThrow(authorizationException);

        final BraintreeHttpResponseParser sut = new BraintreeHttpResponseParser(baseParser);
        sut.parse(123, urlConnection);
    }

    @Test
    public void parse_whenBaseParserThrowsUnprocessibleEntityException_throwsErrorWithResponseException() throws Exception {
        exceptionRule.expect(ErrorWithResponse.class);
        exceptionRule.expectMessage("There was an error");

        UnprocessableEntityException unprocessableEntityException = new UnprocessableEntityException(Fixtures.ERROR_RESPONSE);
        when(baseParser.parse(123, urlConnection)).thenThrow(unprocessableEntityException);

        final BraintreeHttpResponseParser sut = new BraintreeHttpResponseParser(baseParser);
        sut.parse(123, urlConnection);
    }
}