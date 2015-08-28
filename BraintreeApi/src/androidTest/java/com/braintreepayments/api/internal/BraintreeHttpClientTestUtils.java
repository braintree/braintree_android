package com.braintreepayments.api.internal;

import android.os.SystemClock;

import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.models.ClientKey;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;

import static com.braintreepayments.testutils.TestClientKey.CLIENT_KEY;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

public class BraintreeHttpClientTestUtils {

    /**
     * Check if the given {@link BraintreeHttpClient} is idle.
     *
     * @param httpClient
     * @return
     */
    public static boolean isHttpClientIdle(BraintreeHttpClient httpClient) {
        return httpClient.mThreadPool.isIdle();
    }

    public static void waitForHttpClientToIdle(BraintreeHttpClient httpClient) {
        do {
            if (isHttpClientIdle(httpClient)) {
                return;
            }
            SystemClock.sleep(10);
        } while (true);
    }

    public static BraintreeHttpClient clientWithExpectedResponse(int responseCode, String response)
            throws IOException, ErrorWithResponse, InvalidArgumentException {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getOutputStream()).thenReturn(mock(OutputStream.class));
        when(connection.getResponseCode()).thenReturn(responseCode);
        when(connection.getInputStream()).thenReturn(streamFromString(response))
            .thenReturn(streamFromString(response));
        when(connection.getErrorStream()).thenReturn(streamFromString(response))
            .thenReturn(streamFromString(response));

        BraintreeHttpClient httpClient = spy(new BraintreeHttpClient(ClientKey.fromString(CLIENT_KEY)));
        doReturn(connection).when(httpClient).init(anyString());

        return httpClient;
    }

    public static BraintreeHttpClient clientWithExpectedException(Throwable throwable)
            throws IOException, InvalidArgumentException {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getOutputStream()).thenReturn(mock(OutputStream.class));
        when(connection.getResponseCode()).thenThrow(throwable);

        BraintreeHttpClient httpClient = spy(new BraintreeHttpClient(ClientKey.fromString(CLIENT_KEY)));
        doReturn(connection).when(httpClient).init(anyString());

        return httpClient;
    }

    public static InputStream streamFromString(String string) throws UnsupportedEncodingException {
        return new ByteArrayInputStream(string.getBytes("UTF-8"));
    }
}
