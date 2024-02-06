package com.braintreepayments.api

import org.junit.Assert.assertEquals
import org.junit.Test

class PayPalAnalyticsUnitTest {

    @Test
    fun testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals("paypal:tokenize:started", PayPalAnalytics.TOKENIZATION_STARTED)
        assertEquals("paypal:tokenize:failed", PayPalAnalytics.TOKENIZATION_FAILED)
        assertEquals("paypal:tokenize:succeeded", PayPalAnalytics.TOKENIZATION_SUCCEEDED)
        assertEquals(
            "paypal:tokenize:browser-login:canceled",
            PayPalAnalytics.BROWSER_LOGIN_CANCELED
        )
    }
}
