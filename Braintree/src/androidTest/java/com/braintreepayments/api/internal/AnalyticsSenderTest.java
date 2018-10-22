package com.braintreepayments.api.internal;

import androidx.test.runner.AndroidJUnit4;

import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.test.TestClientTokenBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;

import static androidx.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class AnalyticsSenderTest {

    @Test(timeout = 10000)
    public void sendsCorrectlyFormattedAnalyticsRequest() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        String authorization = new TestClientTokenBuilder().withAnalytics().build();
        final Configuration configuration = Configuration.fromJson(authorization);

        AnalyticsEvent event = new AnalyticsEvent(getTargetContext(), "sessionId", "custom", "event.started");
        AnalyticsDatabase.getInstance(getTargetContext()).addEvent(event);

        BraintreeHttpClient httpClient = new BraintreeHttpClient(Authorization.fromString(authorization)) {
            @Override
            protected String parseResponse(HttpURLConnection connection) throws Exception {
                if (connection.getURL().toString().equals(configuration.getAnalytics().getUrl())) {
                    assertEquals(200, connection.getResponseCode());
                    latch.countDown();
                }
                return "";
            }
        };

        AnalyticsSender.send(getTargetContext(), Authorization.fromString(authorization), httpClient,
                configuration.getAnalytics().getUrl(), true);

        latch.await();
    }
}
