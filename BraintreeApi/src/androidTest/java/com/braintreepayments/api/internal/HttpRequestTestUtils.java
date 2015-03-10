package com.braintreepayments.api.internal;

import com.braintreepayments.api.exceptions.ErrorWithResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class HttpRequestTestUtils {

    public static HttpRequest requestWithExpectedResponse(final int responseCode,
            final String response) throws IOException, ErrorWithResponse {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getOutputStream()).thenReturn(mock(OutputStream.class));
        when(connection.getResponseCode()).thenReturn(responseCode);
        when(connection.getInputStream()).thenReturn(streamFromString(response));
        when(connection.getErrorStream()).thenReturn(streamFromString(response));

        HttpRequest httpRequest = spy(new HttpRequest("", ""));
        doReturn(connection).when(httpRequest).init(anyString());

        return httpRequest;
    }

    public static InputStream streamFromString(String string) throws UnsupportedEncodingException {
        return new ByteArrayInputStream(string.getBytes("UTF-8"));
    }
}
