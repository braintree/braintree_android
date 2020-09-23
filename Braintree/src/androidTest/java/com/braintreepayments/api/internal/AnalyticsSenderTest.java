package com.braintreepayments.api.internal;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.test.TestClientTokenBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4ClassRunner.class)
public class AnalyticsSenderTest {

    private static final String ANALYTICS_URL = "https://origin-analytics-sand.sandbox.braintree-api.com/some-merchant-id";

    @Test(timeout = 10000)
    public void sendsCorrectlyFormattedAnalyticsRequest() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        Authorization authorization = Authorization.fromString(new TestClientTokenBuilder().build());
        AnalyticsEvent event = new AnalyticsEvent(ApplicationProvider.getApplicationContext(), "sessionId", "custom", "event.started");
        AnalyticsDatabase.getInstance(ApplicationProvider.getApplicationContext()).addEvent(event);

        BraintreeHttpClient httpClient = new BraintreeHttpClient(authorization) {
            @Override
            protected String parseResponse(HttpURLConnection connection) throws Exception {
                if (ANALYTICS_URL.equals(connection.getURL().toString())) {
                    assertEquals(200, connection.getResponseCode());
                    latch.countDown();
                }
                return null;
            }
        };

        AnalyticsSender.send(ApplicationProvider.getApplicationContext(), authorization, httpClient, ANALYTICS_URL, true);

        latch.await();
    }
}
