package com.braintreepayments.api.internal;

import android.os.SystemClock;

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

    public static BraintreeHttpClient requestWithExpectedResponse(int responseCode, String response)
            throws IOException, ErrorWithResponse {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getOutputStream()).thenReturn(mock(OutputStream.class));
        when(connection.getResponseCode()).thenReturn(responseCode);
        when(connection.getInputStream()).thenReturn(streamFromString(response))
            .thenReturn(streamFromString(response));
        when(connection.getErrorStream()).thenReturn(streamFromString(response))
            .thenReturn(streamFromString(response));

        BraintreeHttpClient httpRequest = spy(new BraintreeHttpClient(null));
        doReturn(connection).when(httpRequest).init(anyString());

        return httpRequest;
    }

    public static BraintreeHttpClient requestWithExpectedException(Throwable throwable)
            throws IOException {
        HttpURLConnection connection = mock(HttpURLConnection.class);
        when(connection.getOutputStream()).thenReturn(mock(OutputStream.class));
        when(connection.getResponseCode()).thenThrow(throwable);

        BraintreeHttpClient httpRequest = spy(new BraintreeHttpClient(null));
        doReturn(connection).when(httpRequest).init(anyString());

        return httpRequest;
    }

    public static InputStream streamFromString(String string) throws UnsupportedEncodingException {
        return new ByteArrayInputStream(string.getBytes("UTF-8"));
    }
}
