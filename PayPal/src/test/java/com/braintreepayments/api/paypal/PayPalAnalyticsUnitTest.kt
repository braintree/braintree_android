package com.braintreepayments.api.paypal

import org.junit.Assert.assertEquals
import org.junit.Test

class PayPalAnalyticsUnitTest {

    @Test
    fun testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals("paypal:tokenize:started", PayPalAnalytics.TOKENIZATION_STARTED)
        assertEquals("paypal:tokenize:failed", PayPalAnalytics.TOKENIZATION_FAILED)
        assertEquals("paypal:tokenize:succeeded", PayPalAnalytics.TOKENIZATION_SUCCEEDED)
        assertEquals(
            "paypal:tokenize:browser-login:canceled",
            PayPalAnalytics.BROWSER_LOGIN_CANCELED
        )
    }

    @Test
    fun testAnalyticsEvents_sendsExpectedEditFiEventNames() {
        assertEquals("paypal:edit:started", PayPalAnalytics.EDIT_FI_STARTED)

        assertEquals(
            "paypal:edit:browser-presentation:succeeded",
            PayPalAnalytics.EDIT_FI_BROWSER_PRESENTATION_SUCCEEDED
        )

        assertEquals(
            "paypal:edit:browser-presentation:failed",
            PayPalAnalytics.EDIT_FI_BROWSER_PRESENTATION_FAILED
        )

        assertEquals(
            "paypal:edit:browser-login:canceled",
            PayPalAnalytics.EDIT_FI_BROWSER_LOGIN_CANCELED
        )

        assertEquals("paypal:edit:failed", PayPalAnalytics.EDIT_FI_FAILED)
        
        assertEquals("paypal:edit:succeeded", PayPalAnalytics.EDIT_FI_SUCCEEDED)
    }
}
