package com.braintreepayments.api

import com.braintreepayments.api.threedsecure.ThreeDSecureAnalytics
import org.junit.Assert.assertEquals
import org.junit.Test

class ThreeDSecureAnalyticsUnitTest {

    @Test
    fun testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals("3ds:verify:started", ThreeDSecureAnalytics.VERIFY_STARTED)
        assertEquals("3ds:verify:succeeded", ThreeDSecureAnalytics.VERIFY_SUCCEEDED)
        assertEquals("3ds:verify:failed", ThreeDSecureAnalytics.VERIFY_FAILED)
        assertEquals("3ds:verify:canceled", ThreeDSecureAnalytics.VERIFY_CANCELED)
        assertEquals(
            "3ds:verify:lookup:succeeded",
            ThreeDSecureAnalytics.LOOKUP_SUCCEEDED
        )
        assertEquals("3ds:verify:lookup:failed", ThreeDSecureAnalytics.LOOKUP_FAILED)
        assertEquals(
            "3ds:verify:lookup:challenge-required",
            ThreeDSecureAnalytics.CHALLENGE_REQUIRED
        )
        assertEquals(
            "3ds:verify:authenticate-jwt:succeeded",
            ThreeDSecureAnalytics.JWT_AUTH_SUCCEEDED
        )
        assertEquals(
            "3ds:verify:authenticate-jwt:failed",
            ThreeDSecureAnalytics.JWT_AUTH_FAILED
        )
    }
}
