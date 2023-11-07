package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AmericanExpressAnalyticsUnitTest {

    @Test
    public void testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals("amex:rewards-balance:started",
                AmericanExpressAnalytics.REWARDS_BALANCE_STARTED.getEvent());
        assertEquals("amex:rewards-balance:failed",
                AmericanExpressAnalytics.REWARDS_BALANCE_FAILED.getEvent());
        assertEquals("amex:rewards-balance:succeeded",
                AmericanExpressAnalytics.REWARDS_BALANCE_SUCCEEDED.getEvent());
    }
}
