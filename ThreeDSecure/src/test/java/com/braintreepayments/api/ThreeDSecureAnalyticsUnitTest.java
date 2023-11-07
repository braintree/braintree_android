package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ThreeDSecureAnalyticsUnitTest {

    @Test
    public void testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals("3ds:verify:started", ThreeDSecureAnalytics.VERIFY_STARTED.getEvent());
        assertEquals("3ds:verify:succeeded", ThreeDSecureAnalytics.VERIFY_SUCCEEDED.getEvent());
        assertEquals("3ds:verify:failed", ThreeDSecureAnalytics.VERIFY_FAILED.getEvent());
        assertEquals("3ds:verify:canceled", ThreeDSecureAnalytics.VERIFY_CANCELED.getEvent());
        assertEquals("3ds:verify:lookup:succeeded",
                ThreeDSecureAnalytics.LOOKUP_SUCCEEDED.getEvent());
        assertEquals("3ds:verify:lookup:failed", ThreeDSecureAnalytics.LOOKUP_FAILED.getEvent());
        assertEquals("3ds:verify:lookup:challenge-required",
                ThreeDSecureAnalytics.CHALLENGE_REQUIRED.getEvent());
        assertEquals("3ds:verify:lookup:network-connection:failed",
                ThreeDSecureAnalytics.NETWORK_CONNECTION_LOST.getEvent());
        assertEquals("3ds:verify:challenge.succeeded",
                ThreeDSecureAnalytics.CHALLENGE_SUCCEEDED.getEvent());
        assertEquals("3ds:verify:challenge.failed",
                ThreeDSecureAnalytics.CHALLENGE_FAILED.getEvent());
        assertEquals("3ds:verify:authenticate-jwt:succeeded",
                ThreeDSecureAnalytics.JWT_AUTH_SUCCEEDED.getEvent());
        assertEquals("3ds:verify:authenticate-jwt:failed",
                ThreeDSecureAnalytics.JWT_AUTH_FAILED.getEvent());
    }
}
