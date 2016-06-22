package com.braintreepayments.api.internal;

import android.content.Intent;
import android.support.test.rule.ServiceTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.testutils.TestClientTokenBuilder;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.HttpURLConnection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class AnalyticsIntentServiceTest {

    @Rule
    public ServiceTestRule mServiceTestRule = new ServiceTestRule();

    private Intent mServiceIntent;
    private String mAuthorization;

    @Before
    public void setup() throws InvalidArgumentException {
        mAuthorization = new TestClientTokenBuilder().withAnalytics().build();
        mServiceIntent = new Intent(getTargetContext(), AnalyticsIntentService.class)
                .putExtra(AnalyticsIntentService.EXTRA_AUTHORIZATION, mAuthorization)
                .putExtra(AnalyticsIntentService.EXTRA_CONFIGURATION, mAuthorization);
    }

    @Test(timeout = 10000)
    public void sendsCorrectlyFormattedAnalyticsRequest()
            throws InvalidArgumentException, InterruptedException, TimeoutException, JSONException {
        AnalyticsEvent event = new AnalyticsEvent(getTargetContext(), "sessionId", "custom", "event.started");
        AnalyticsDatabase.getInstance(getTargetContext()).addEvent(event);

        AnalyticsIntentService service = new AnalyticsIntentService();
        service.mContext = getTargetContext();

        final CountDownLatch latch = new CountDownLatch(1);
        final Configuration configuration = Configuration.fromJson(mAuthorization);

        service.mHttpClient = new BraintreeHttpClient(Authorization.fromString(mAuthorization)) {
            @Override
            protected String parseResponse(HttpURLConnection connection) throws Exception {
                if (connection.getURL().toString().equals(configuration.getAnalytics().getUrl())) {
                    assertEquals(200, connection.getResponseCode());
                    latch.countDown();
                }
                return "";
            }
        };

        service.onHandleIntent(mServiceIntent);
        latch.await();
    }
}
