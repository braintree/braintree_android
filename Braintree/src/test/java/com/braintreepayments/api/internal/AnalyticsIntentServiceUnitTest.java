package com.braintreepayments.api.internal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

@RunWith(RobolectricGradleTestRunner.class)
public class AnalyticsIntentServiceUnitTest {

    @Test
    public void handlesNullIntent() {
        AnalyticsIntentService service = new AnalyticsIntentService();
        service.onCreate();

        service.onHandleIntent(null);
    }
}
