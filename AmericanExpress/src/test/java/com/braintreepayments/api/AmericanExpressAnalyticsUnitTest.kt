package com.braintreepayments.api

import com.braintreepayments.americanexpress.AmericanExpressAnalytics
import org.junit.Assert.assertEquals
import org.junit.Test

class AmericanExpressAnalyticsUnitTest {

    @Test
    fun testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals(
            "amex:rewards-balance:started",
            AmericanExpressAnalytics.REWARDS_BALANCE_STARTED
        )
        assertEquals(
            "amex:rewards-balance:failed",
            AmericanExpressAnalytics.REWARDS_BALANCE_FAILED
        )
        assertEquals(
            "amex:rewards-balance:succeeded",
            AmericanExpressAnalytics.REWARDS_BALANCE_SUCCEEDED
        )
    }
}
