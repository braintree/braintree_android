package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VenmoAnalyticsUnitTest {

    @Test
    public void testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals("venmo:tokenize:app-switch:canceled",
                VenmoAnalytics.APP_SWITCH_CANCELED.getEvent());
        assertEquals("venmo:tokenize:app-switch:failed",
                VenmoAnalytics.APP_SWITCH_FAILED.getEvent());
        assertEquals("venmo:tokenize:app-switch:succeeded",
                VenmoAnalytics.APP_SWITCH_SUCCEEDED.getEvent());
        assertEquals("venmo:tokenize:failed", VenmoAnalytics.TOKENIZE_FAILED.getEvent());
        assertEquals("venmo:tokenize:started", VenmoAnalytics.TOKENIZE_STARTED.getEvent());
        assertEquals("venmo:tokenize:succeeded", VenmoAnalytics.TOKENIZE_SUCCEEDED.getEvent());
        assertEquals("venmo:tokenize:network-connection:failed",
                VenmoAnalytics.TOKENIZE_NETWORK_CONNECTION_LOST.getEvent());
    }

}
