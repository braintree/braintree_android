package com.braintreepayments.api

import org.junit.Assert.assertEquals
import org.junit.Test

class VenmoAnalyticsUnitTest {
    @Test
    fun testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals(
            "venmo:tokenize:app-switch:canceled",
            VenmoAnalytics.APP_SWITCH_CANCELED.event
        )
        assertEquals(
            "venmo:tokenize:app-switch:failed",
            VenmoAnalytics.APP_SWITCH_FAILED.event
        )
        assertEquals(
            "venmo:tokenize:app-switch:succeeded",
            VenmoAnalytics.APP_SWITCH_SUCCEEDED.event
        )
        assertEquals("venmo:tokenize:failed", VenmoAnalytics.TOKENIZE_FAILED.event)
        assertEquals("venmo:tokenize:started", VenmoAnalytics.TOKENIZE_STARTED.event)
        assertEquals("venmo:tokenize:succeeded", VenmoAnalytics.TOKENIZE_SUCCEEDED.event)
        assertEquals(
            "venmo:tokenize:network-connection:failed",
            VenmoAnalytics.TOKENIZE_NETWORK_CONNECTION_LOST.event
        )
    }
}
