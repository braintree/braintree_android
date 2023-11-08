package com.braintreepayments.api

import org.junit.Assert.assertEquals
import org.junit.Test

class GooglePayAnalyticsUnitTest {

    @Test
    fun testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals(
            "google-pay:payment-request:started",
            GooglePayAnalytics.PAYMENT_REQUEST_STARTED.event
        )
        assertEquals(
            "google-pay:payment-request:failed",
            GooglePayAnalytics.PAYMENT_REQUEST_FAILED.event
        )
        assertEquals(
            "google-pay:payment-request:succeeded",
            GooglePayAnalytics.PAYMENT_REQUEST_SUCCEEDED.event
        )
        assertEquals(
            "google-pay:tokenize:started",
            GooglePayAnalytics.TOKENIZE_STARTED.event
        )
        assertEquals(
            "google-pay:tokenize:failed",
            GooglePayAnalytics.TOKENIZE_FAILED.event
        )
        assertEquals(
            "google-pay:tokenize:succeeded",
            GooglePayAnalytics.TOKENIZE_SUCCEEDED.event
        )
    }
}
