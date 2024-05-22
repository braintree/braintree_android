package com.braintreepayments.api.venmo

import org.junit.Assert.assertEquals
import org.junit.Test

class VenmoAnalyticsUnitTest {

    @Test
    fun testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals(
            "venmo:tokenize:app-switch:canceled",
            VenmoAnalytics.APP_SWITCH_CANCELED
        )
        assertEquals(
            "venmo:tokenize:app-switch:failed",
            VenmoAnalytics.APP_SWITCH_FAILED
        )
        assertEquals(
            "venmo:tokenize:app-switch:succeeded",
            VenmoAnalytics.APP_SWITCH_SUCCEEDED
        )
        assertEquals("venmo:tokenize:failed", VenmoAnalytics.TOKENIZE_FAILED)
        assertEquals("venmo:tokenize:started", VenmoAnalytics.TOKENIZE_STARTED)
        assertEquals("venmo:tokenize:succeeded", VenmoAnalytics.TOKENIZE_SUCCEEDED)
    }
}
