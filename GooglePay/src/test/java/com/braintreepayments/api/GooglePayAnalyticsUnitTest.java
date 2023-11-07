package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GooglePayAnalyticsUnitTest {

    @Test
    public void testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals("google-pay:payment-request:started",
                GooglePayAnalytics.PAYMENT_REQUEST_STARTED.getEvent());
        assertEquals("google-pay:payment-request:failed",
                GooglePayAnalytics.PAYMENT_REQUEST_FAILED.getEvent());
        assertEquals("google-pay:payment-request:succeeded",
                GooglePayAnalytics.PAYMENT_REQUEST_SUCCEEDED.getEvent());
        assertEquals("google-pay:tokenize:started", GooglePayAnalytics.TOKENIZE_STARTED.getEvent());
        assertEquals("google-pay:tokenize:failed", GooglePayAnalytics.TOKENIZE_FAILED.getEvent());
        assertEquals("google-pay:tokenize:network-connection:failed",
                GooglePayAnalytics.TOKENIZE_NETWORK_CONNECTION_LOST.getEvent());
        assertEquals("google-pay:tokenize:succeeded",
                GooglePayAnalytics.TOKENIZE_SUCCEEDED.getEvent());
    }
}
