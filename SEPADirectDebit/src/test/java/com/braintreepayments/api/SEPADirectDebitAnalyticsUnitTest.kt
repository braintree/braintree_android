package com.braintreepayments.api

import org.junit.Assert.assertEquals
import org.junit.Test

class SEPADirectDebitAnalyticsUnitTest {

    @Test
    fun testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals(
            "sepa:tokenize:started",
            SEPADirectDebitAnalytics.TOKENIZE_STARTED.event
        )
        assertEquals(
            "sepa:tokenize:succeeded",
            SEPADirectDebitAnalytics.TOKENIZE_SUCCEEDED.event
        )
        assertEquals("sepa:tokenize:failed", SEPADirectDebitAnalytics.TOKENIZE_FAILED.event)
        assertEquals(
            "sepa:tokenize:challenge:canceled",
            SEPADirectDebitAnalytics.CHALLENGE_CANCELED.event
        )
        assertEquals(
            "sepa:tokenize:create-mandate:challenge-required",
            SEPADirectDebitAnalytics.CREATE_MANDATE_CHALLENGE_REQUIRED.event
        )
        assertEquals(
            "sepa:tokenize:create-mandate:succeeded",
            SEPADirectDebitAnalytics.CREATE_MANDATE_SUCCEEDED.event
        )
        assertEquals(
            "sepa:tokenize:create-mandate:failed",
            SEPADirectDebitAnalytics.CREATE_MANDATE_FAILED.event
        )
        assertEquals(
            "sepa:tokenize:challenge-presentation:succeeded",
            SEPADirectDebitAnalytics.CHALLENGE_PRESENTATION_SUCCEEDED.event
        )
        assertEquals(
            "sepa:tokenize:challenge-presentation:failed",
            SEPADirectDebitAnalytics.CHALLENGE_PRESENTATION_FAILED.event
        )
        assertEquals(
            "sepa:tokenize:challenge:succeeded",
            SEPADirectDebitAnalytics.CHALLENGE_SUCCEEDED.event
        )
        assertEquals(
            "sepa:tokenize:challenge:failed",
            SEPADirectDebitAnalytics.CHALLENGE_FAILED.event
        )
    }
}
