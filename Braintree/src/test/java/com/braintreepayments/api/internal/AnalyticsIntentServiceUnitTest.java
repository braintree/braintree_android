package com.braintreepayments.api.internal;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AnalyticsIntentServiceUnitTest {

    @Test
    public void handlesNullIntent() {
        AnalyticsIntentService service = new AnalyticsIntentService();
        service.onCreate();

        service.onHandleIntent(null);
    }
}
