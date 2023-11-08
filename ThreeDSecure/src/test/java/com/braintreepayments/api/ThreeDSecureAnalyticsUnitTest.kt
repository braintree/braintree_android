package com.braintreepayments.api

import org.junit.Assert.assertEquals
import org.junit.Test

class ThreeDSecureAnalyticsUnitTest {
    @Test
    fun testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals("3ds:verify:started", ThreeDSecureAnalytics.VERIFY_STARTED.event)
        assertEquals("3ds:verify:succeeded", ThreeDSecureAnalytics.VERIFY_SUCCEEDED.event)
        assertEquals("3ds:verify:failed", ThreeDSecureAnalytics.VERIFY_FAILED.event)
        assertEquals("3ds:verify:canceled", ThreeDSecureAnalytics.VERIFY_CANCELED.event)
        assertEquals(
            "3ds:verify:lookup:succeeded",
            ThreeDSecureAnalytics.LOOKUP_SUCCEEDED.event
        )
        assertEquals("3ds:verify:lookup:failed", ThreeDSecureAnalytics.LOOKUP_FAILED.event)
        assertEquals(
            "3ds:verify:lookup:challenge-required",
            ThreeDSecureAnalytics.CHALLENGE_REQUIRED.event
        )
        assertEquals(
            "3ds:verify:challenge.succeeded",
            ThreeDSecureAnalytics.CHALLENGE_SUCCEEDED.event
        )
        assertEquals(
            "3ds:verify:challenge.failed",
            ThreeDSecureAnalytics.CHALLENGE_FAILED.event
        )
        assertEquals(
            "3ds:verify:authenticate-jwt:succeeded",
            ThreeDSecureAnalytics.JWT_AUTH_SUCCEEDED.event
        )
        assertEquals(
            "3ds:verify:authenticate-jwt:failed",
            ThreeDSecureAnalytics.JWT_AUTH_FAILED.event
        )
    }
}
