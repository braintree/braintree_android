package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LocalPaymentAnalyticsUnitTest {

    @Test
    public void testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals("local-payment:start-payment:started",
                LocalPaymentAnalytics.PAYMENT_STARTED.getEvent());
        assertEquals("local-payment:start-payment:succeeded",
                LocalPaymentAnalytics.PAYMENT_SUCCEEDED.getEvent());
        assertEquals("local-payment:start-payment:failed",
                LocalPaymentAnalytics.PAYMENT_FAILED.getEvent());
        assertEquals("local-payment:start-payment:browser-login:canceled",
                LocalPaymentAnalytics.PAYMENT_CANCELED.getEvent());
        assertEquals("local-payment:start-payment:browser-presentation:succeeded",
                LocalPaymentAnalytics.BROWSER_PRESENTATION_SUCCEEDED.getEvent());
        assertEquals("local-payment:start-payment:browser-presentation:failed",
                LocalPaymentAnalytics.BROWSER_PRESENTATION_FAILED.getEvent());
        assertEquals("local-payment:start-payment:browser-login:failed",
                LocalPaymentAnalytics.BROWSER_LOGIN_FAILED.getEvent());
        assertEquals("local-payment:start-payment:network-connection:failed",
                LocalPaymentAnalytics.PAYMENT_NETWORK_CONNECTION_LOST.getEvent());
    }
}
