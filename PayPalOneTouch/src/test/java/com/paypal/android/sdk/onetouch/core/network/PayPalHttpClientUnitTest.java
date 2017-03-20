package com.paypal.android.sdk.onetouch.core.network;

import com.paypal.android.sdk.onetouch.core.BuildConfig;
import com.paypal.android.sdk.onetouch.core.base.DeviceInspector;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.net.HttpURLConnection;

import static com.paypal.android.sdk.onetouch.core.base.DeviceInspector.getDeviceName;
import static junit.framework.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class PayPalHttpClientUnitTest {

    @Test
    public void setsUserAgent() throws IOException {
        PayPalHttpClient httpClient = new PayPalHttpClient();

        HttpURLConnection connection = httpClient.init("http://example.com");

        String userAgent = String.format("PayPalSDK/PayPalOneTouch-Android %s (%s; %s; %s)", BuildConfig.VERSION_NAME,
                DeviceInspector.getOs(), getDeviceName(), BuildConfig.DEBUG ? "debug;" : "");
        assertEquals(userAgent, connection.getRequestProperty("User-Agent"));
    }

    @Test
    public void setsConnectTimeout() throws IOException {
        PayPalHttpClient httpClient = new PayPalHttpClient();

        HttpURLConnection connection = httpClient.init("http://example.com");

        assertEquals(90000, connection.getConnectTimeout());
    }
}
