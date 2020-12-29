package com.braintreepayments.api.internal;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner;

import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.test.TestClientTokenBuilder;
import com.braintreepayments.testutils.Fixtures;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4ClassRunner.class)
public class AnalyticsSenderTest {

    private static final String PROD_ANALYTICS_URL = "https://client-analytics.braintreegateway.com/some-merchant-id";
    private static final String SANDBOX_ANALYTICS_URL = "https://origin-analytics-sand.sandbox.braintree-api.com/some-merchant-id";

    private Context context;

    @Before
    public void beforeEach() {
        context = ApplicationProvider.getApplicationContext();
    }

    @Test(timeout = 10000)
    public void sendsCorrectlyFormattedAnalyticsRequestToSandbox() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        Authorization authorization = Authorization.fromString(new TestClientTokenBuilder().build());
        AnalyticsEvent event = new AnalyticsEvent(context, "sessionId", "custom", "event.started");
        AnalyticsDatabase.getInstance(context).addEvent(event);

        BraintreeHttpClient httpClient = new BraintreeHttpClient(authorization) {

            @Override
            public String post(String path, String data, Configuration configuration) throws Exception {
                String response = null;
                try {
                    response = super.post(path, data, configuration);
                    latch.countDown();
                } catch (Exception e) {
                    fail("request should execute successfully");
                }
                return response;
            }
        };

        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_SANDBOX_ANALYTICS);
        AnalyticsSender.send(context, authorization, configuration, httpClient, SANDBOX_ANALYTICS_URL, true);

        latch.await();
    }

    @Test(timeout = 10000)
    public void sendsCorrectlyFormattedAnalyticsRequestToProd() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        Authorization authorization = Authorization.fromString(new TestClientTokenBuilder().build());
        AnalyticsEvent event = new AnalyticsEvent(context, "sessionId", "custom", "event.started");
        AnalyticsDatabase.getInstance(context).addEvent(event);

        BraintreeHttpClient httpClient = new BraintreeHttpClient(authorization) {

            @Override
            public String post(String path, String data, Configuration configuration) throws Exception {
                String response = null;
                try {
                    response = super.post(path, data, configuration);
                    latch.countDown();
                } catch (Exception e) {
                    fail("request should execute successfully");
                }
                return response;
            }
        };

        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PROD_ANALYTICS);
        AnalyticsSender.send(context, authorization, configuration, httpClient, PROD_ANALYTICS_URL, true);

        latch.await();
    }
}
