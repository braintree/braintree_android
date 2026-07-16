package com.braintreepayments.api.visacheckout

import org.junit.Assert.assertEquals
import org.junit.Test

class VisaCheckoutAnalyticsUnitTest {

    @Test
    fun `analytics event constants have expected event names`() {
        assertEquals("visa-checkout:tokenize:started", VisaCheckoutAnalytics.TOKENIZE_STARTED)
        assertEquals("visa-checkout:tokenize:failed", VisaCheckoutAnalytics.TOKENIZE_FAILED)
        assertEquals("visa-checkout:tokenize:succeeded", VisaCheckoutAnalytics.TOKENIZE_SUCCEEDED)
    }
}
