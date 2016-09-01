package com.braintreepayments.api.internal;

import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.models.TokenizationKey;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import static com.braintreepayments.testutils.FixturesHelper.streamFromString;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class BraintreeHttpClientTestUtils {

    public static BraintreeHttpClient clientWithExpectedResponse(int responseCode, String response)
            throws IOException, ErrorWithResponse, InvalidArgumentException {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getOutputStream()).thenReturn(mock(OutputStream.class));
        when(connection.getResponseCode()).thenReturn(responseCode);
        when(connection.getInputStream()).thenReturn(streamFromString(response))
            .thenReturn(streamFromString(response));
        when(connection.getErrorStream()).thenReturn(streamFromString(response))
            .thenReturn(streamFromString(response));

        BraintreeHttpClient httpClient = spy(new BraintreeHttpClient(
                TokenizationKey.fromString(TOKENIZATION_KEY)));
        doReturn(connection).when(httpClient).init(anyString());

        return httpClient;
    }
}
