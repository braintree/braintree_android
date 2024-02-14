package com.braintreepayments.api

import org.junit.Assert.assertEquals
import org.junit.Test

class SEPADirectDebitAnalyticsUnitTest {

    @Test
    fun testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals(
            "sepa:tokenize:started",
            SEPADirectDebitAnalytics.TOKENIZE_STARTED
        )
        assertEquals(
            "sepa:tokenize:succeeded",
            SEPADirectDebitAnalytics.TOKENIZE_SUCCEEDED
        )
        assertEquals("sepa:tokenize:failed", SEPADirectDebitAnalytics.TOKENIZE_FAILED)
        assertEquals(
            "sepa:tokenize:challenge:canceled",
            SEPADirectDebitAnalytics.CHALLENGE_CANCELED
        )
        assertEquals(
            "sepa:tokenize:create-mandate:challenge-required",
            SEPADirectDebitAnalytics.CREATE_MANDATE_CHALLENGE_REQUIRED
        )
        assertEquals(
            "sepa:tokenize:create-mandate:succeeded",
            SEPADirectDebitAnalytics.CREATE_MANDATE_SUCCEEDED
        )
        assertEquals(
            "sepa:tokenize:create-mandate:failed",
            SEPADirectDebitAnalytics.CREATE_MANDATE_FAILED
        )
        assertEquals(
            "sepa:tokenize:challenge:succeeded",
            SEPADirectDebitAnalytics.CHALLENGE_SUCCEEDED
        )
        assertEquals(
            "sepa:tokenize:challenge:failed",
            SEPADirectDebitAnalytics.CHALLENGE_FAILED
        )
    }
}
