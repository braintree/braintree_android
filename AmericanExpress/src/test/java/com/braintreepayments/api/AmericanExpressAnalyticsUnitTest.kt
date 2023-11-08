package com.braintreepayments.api

import org.junit.Assert.assertEquals
import org.junit.Test

class AmericanExpressAnalyticsUnitTest {
    @Test
    fun testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals(
            "amex:rewards-balance:started",
            AmericanExpressAnalytics.REWARDS_BALANCE_STARTED.event
        )
        assertEquals(
            "amex:rewards-balance:failed",
            AmericanExpressAnalytics.REWARDS_BALANCE_FAILED.event
        )
        assertEquals(
            "amex:rewards-balance:succeeded",
            AmericanExpressAnalytics.REWARDS_BALANCE_SUCCEEDED.event
        )
    }
}
