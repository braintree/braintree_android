package com.braintreepayments.api.internal;

import androidx.test.runner.AndroidJUnit4;

import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.test.TestClientTokenBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;

import static androidx.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class AnalyticsSenderTest {

    private static final String ANALYTICS_URL = "https://origin-analytics-sand.sandbox.braintree-api.com/some-merchant-id";

    @Test(timeout = 10000)
    public void sendsCorrectlyFormattedAnalyticsRequest() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        Authorization authorization = Authorization.fromString(new TestClientTokenBuilder().build());
        AnalyticsEvent event = new AnalyticsEvent(getTargetContext(), "sessionId", "custom", "event.started");
        AnalyticsDatabase.getInstance(getTargetContext()).addEvent(event);

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

        AnalyticsSender.send(getTargetContext(), authorization, httpClient, ANALYTICS_URL, true);

        latch.await();
    }
}
