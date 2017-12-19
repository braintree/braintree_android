package com.braintreepayments.api.internal;

import java.io.OutputStream;
import java.net.HttpURLConnection;

import static com.braintreepayments.testutils.FixturesHelper.streamFromString;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class HttpClientTestUtils {

    public static HttpClient stubResponse(HttpClient client, int responseCode, String response) throws Exception {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getOutputStream()).thenReturn(mock(OutputStream.class));
        when(connection.getResponseCode()).thenReturn(responseCode);
        when(connection.getInputStream()).thenReturn(streamFromString(response))
            .thenReturn(streamFromString(response));
        when(connection.getErrorStream()).thenReturn(streamFromString(response))
            .thenReturn(streamFromString(response));

        HttpClient httpClient = spy(client);
        doReturn(connection).when(httpClient).init(anyString());

        return httpClient;
    }
}
