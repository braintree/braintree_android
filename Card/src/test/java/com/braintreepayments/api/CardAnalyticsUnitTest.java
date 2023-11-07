package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CardAnalyticsUnitTest {

    @Test
    public void testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals("card:tokenize:started", CardAnalytics.CARD_TOKENIZE_STARTED.getEvent());
        assertEquals("card:tokenize:failed", CardAnalytics.CARD_TOKENIZE_FAILED.getEvent());
        assertEquals("card:tokenize:succeeded", CardAnalytics.CARD_TOKENIZE_SUCCEEDED.getEvent());
        assertEquals("card:tokenize:network-connection:failed",
                CardAnalytics.CARD_TOKENIZE_NETWORK_CONNECTION_LOST.getEvent());
    }
}
