package com.braintreepayments.api

import org.junit.Assert.assertEquals
import org.junit.Test

class CardAnalyticsUnitTest {

    @Test
    fun testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals("card:tokenize:started", CardAnalytics.CARD_TOKENIZE_STARTED.event)
        assertEquals("card:tokenize:failed", CardAnalytics.CARD_TOKENIZE_FAILED.event)
        assertEquals("card:tokenize:succeeded", CardAnalytics.CARD_TOKENIZE_SUCCEEDED.event)
    }
}
