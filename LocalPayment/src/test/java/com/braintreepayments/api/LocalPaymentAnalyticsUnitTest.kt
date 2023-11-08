package com.braintreepayments.api

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalPaymentAnalyticsUnitTest {
    @Test
    fun testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals(
            "local-payment:start-payment:started",
            LocalPaymentAnalytics.PAYMENT_STARTED.event
        )
        assertEquals(
            "local-payment:start-payment:succeeded",
            LocalPaymentAnalytics.PAYMENT_SUCCEEDED.event
        )
        assertEquals(
            "local-payment:start-payment:failed",
            LocalPaymentAnalytics.PAYMENT_FAILED.event
        )
        assertEquals(
            "local-payment:start-payment:browser-login:canceled",
            LocalPaymentAnalytics.PAYMENT_CANCELED.event
        )
        assertEquals(
            "local-payment:start-payment:browser-presentation:succeeded",
            LocalPaymentAnalytics.BROWSER_SWITCH_SUCCEEDED.event
        )
        assertEquals(
            "local-payment:start-payment:browser-presentation:failed",
            LocalPaymentAnalytics.BROWSER_SWITCH_FAILED.event
        )
        assertEquals(
            "local-payment:start-payment:browser-login:failed",
            LocalPaymentAnalytics.BROWSER_LOGIN_FAILED.event
        )
    }
}
