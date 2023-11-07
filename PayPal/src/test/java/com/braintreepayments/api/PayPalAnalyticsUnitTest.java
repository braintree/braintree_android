package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PayPalAnalyticsUnitTest {

    @Test
    public void testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals("paypal:tokenize:started", PayPalAnalytics.TOKENIZATION_STARTED.getEvent());
        assertEquals("paypal:tokenize:failed", PayPalAnalytics.TOKENIZATION_FAILED.getEvent());
        assertEquals("paypal:tokenize:succeeded",
                PayPalAnalytics.TOKENIZATION_SUCCEEDED.getEvent());
        assertEquals("paypal:tokenize:network-connection:failed",
                PayPalAnalytics.TOKENIZATION_NETWORK_CONNECTION_FAILED.getEvent());
        assertEquals("paypal:tokenize:browser-presentation:succeeded",
                PayPalAnalytics.BROWSER_PRESENTATION_SUCCEEDED.getEvent());
        assertEquals("paypal:tokenize:browser-presentation:failed",
                PayPalAnalytics.BROWSER_PRESENTATION_FAILED.getEvent());
        assertEquals("paypal:tokenize:browser-login:canceled",
                PayPalAnalytics.BROWSER_LOGIN_CANCELED.getEvent());
    }
}
