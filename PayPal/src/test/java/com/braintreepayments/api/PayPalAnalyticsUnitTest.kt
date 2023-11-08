package com.braintreepayments.api

import org.junit.Assert.assertEquals
import org.junit.Test

class PayPalAnalyticsUnitTest {
    @Test
    fun testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals("paypal:tokenize:started", PayPalAnalytics.TOKENIZATION_STARTED.event)
        assertEquals("paypal:tokenize:failed", PayPalAnalytics.TOKENIZATION_FAILED.event)
        assertEquals(
            "paypal:tokenize:succeeded",
            PayPalAnalytics.TOKENIZATION_SUCCEEDED.event
        )
        assertEquals(
            "paypal:tokenize:browser-presentation:succeeded",
            PayPalAnalytics.BROWSER_SWITCH_SUCCEEDED.event
        )
        assertEquals(
            "paypal:tokenize:browser-presentation:failed",
            PayPalAnalytics.BROWSER_SWITCH_FAILED.event
        )
        assertEquals(
            "paypal:tokenize:browser-login:canceled",
            PayPalAnalytics.BROWSER_LOGIN_CANCELED.event
        )
    }
}
