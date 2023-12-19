package com.braintreepayments.api

import org.junit.Assert.assertEquals
import org.junit.Test

class GooglePayAnalyticsUnitTest {

    @Test
    fun testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals(
            "google-pay:payment-request:started",
            GooglePayAnalytics.PAYMENT_REQUEST_STARTED
        )
        assertEquals(
            "google-pay:payment-request:failed",
            GooglePayAnalytics.PAYMENT_REQUEST_FAILED
        )
        assertEquals(
            "google-pay:payment-request:succeeded",
            GooglePayAnalytics.PAYMENT_REQUEST_SUCCEEDED
        )
        assertEquals(
            "google-pay:tokenize:started",
            GooglePayAnalytics.TOKENIZE_STARTED
        )
        assertEquals(
            "google-pay:tokenize:failed",
            GooglePayAnalytics.TOKENIZE_FAILED
        )
        assertEquals(
            "google-pay:tokenize:succeeded",
            GooglePayAnalytics.TOKENIZE_SUCCEEDED
        )
        assertEquals(
            "google-pay:payment-sheet:canceled",
            GooglePayAnalytics.PAYMENT_SHEET_CANCELED
        )
    }
}
