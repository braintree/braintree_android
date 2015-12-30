package com.paypal.android.sdk.onetouch.core.network;

import com.paypal.android.sdk.onetouch.core.BuildConfig;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.net.HttpURLConnection;

import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricGradleTestRunner.class)
@Config(sdk = 16, constants = BuildConfig.class)
public class PayPalHttpClientTest {

    @Test
    public void setsUserAgent() throws IOException {
        PayPalHttpClient httpClient = new PayPalHttpClient();

        HttpURLConnection connection = httpClient.init("http://example.com");

        assertEquals(OtcEnvironment.getUserAgent(),
                connection.getRequestProperty("User-Agent"));
    }

    @Test
    public void setsConnectTimeout() throws IOException {
        PayPalHttpClient httpClient = new PayPalHttpClient();

        HttpURLConnection connection = httpClient.init("http://example.com");

        assertEquals(90000, connection.getConnectTimeout());
    }
}
