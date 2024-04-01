package com.braintreepayments.api.visacheckout

import com.braintreepayments.api.visacheckout.VisaCheckoutAnalytics
import org.junit.Assert.assertEquals
import org.junit.Test

class VisaCheckoutAnalyticsUnitTest {

    @Test
    fun testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals("visa-checkout:tokenize:started", VisaCheckoutAnalytics.TOKENIZE_STARTED)
        assertEquals("visa-checkout:tokenize:failed", VisaCheckoutAnalytics.TOKENIZE_FAILED)
        assertEquals("visa-checkout:tokenize:succeeded", VisaCheckoutAnalytics.TOKENIZE_SUCCEEDED)
    }
}
