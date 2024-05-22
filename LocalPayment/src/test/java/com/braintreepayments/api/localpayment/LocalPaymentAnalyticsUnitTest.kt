package com.braintreepayments.api.localpayment

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalPaymentAnalyticsUnitTest {

    @Test
    fun testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals(
            "local-payment:start-payment:started",
            LocalPaymentAnalytics.PAYMENT_STARTED
        )
        assertEquals(
            "local-payment:start-payment:succeeded",
            LocalPaymentAnalytics.PAYMENT_SUCCEEDED
        )
        assertEquals(
            "local-payment:start-payment:failed",
            LocalPaymentAnalytics.PAYMENT_FAILED
        )
        assertEquals(
            "local-payment:start-payment:browser-login:canceled",
            LocalPaymentAnalytics.PAYMENT_CANCELED
        )
        assertEquals(
            "local-payment:start-payment:browser-presentation:succeeded",
            LocalPaymentAnalytics.BROWSER_SWITCH_SUCCEEDED
        )
    }
}
