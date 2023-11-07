package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SEPADirectDebitAnalyticsUnitTest {

    @Test
    public void testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals("sepa:tokenize:started", SEPADirectDebitAnalytics.TOKENIZE_STARTED.getEvent());
        assertEquals("sepa:tokenize:succeeded",
                SEPADirectDebitAnalytics.TOKENIZE_SUCCEEDED.getEvent());
        assertEquals("sepa:tokenize:failed", SEPADirectDebitAnalytics.TOKENIZE_FAILED.getEvent());
        assertEquals("sepa:tokenize:challenge:canceled",
                SEPADirectDebitAnalytics.CHALLENGE_CANCELED.getEvent());
        assertEquals("sepa:tokenize:create-mandate:challenge-required",
                SEPADirectDebitAnalytics.CREATE_MANDATE_CHALLENGE_REQUIRED.getEvent());
        assertEquals("sepa:tokenize:create-mandate:succeeded",
                SEPADirectDebitAnalytics.CREATE_MANDATE_SUCCEEDED.getEvent());
        assertEquals("sepa:tokenize:create-mandate:failed",
                SEPADirectDebitAnalytics.CREATE_MANDATE_FAILED.getEvent());
        assertEquals("sepa:tokenize:challenge-presentation:succeeded",
                SEPADirectDebitAnalytics.CHALLENGE_PRESENTATION_SUCCEEDED.getEvent());
        assertEquals("sepa:tokenize:challenge-presentation:failed",
                SEPADirectDebitAnalytics.CHALLENGE_PRESENTATION_FAILED.getEvent());
        assertEquals("sepa:tokenize:challenge:alert-canceled",
                SEPADirectDebitAnalytics.CHALLENGE_ALERT_CANCELED.getEvent());
        assertEquals("sepa:tokenize:challenge:succeeded",
                SEPADirectDebitAnalytics.CHALLENGE_SUCCEEDED.getEvent());
        assertEquals("sepa:tokenize:challenge:failed",
                SEPADirectDebitAnalytics.CHALLENGE_FAILED.getEvent());
    }
}
