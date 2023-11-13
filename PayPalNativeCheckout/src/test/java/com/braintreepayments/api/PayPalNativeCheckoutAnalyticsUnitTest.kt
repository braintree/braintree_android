package com.braintreepayments.api

import org.junit.Assert.assertEquals
import org.junit.Test

class PayPalNativeCheckoutAnalyticsUnitTest {

    @Test
    fun testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals(
            "paypal-native:tokenize:started",
            PayPalNativeCheckoutAnalytics.TOKENIZATION_STARTED.event
        )
        assertEquals(
            "paypal-native:tokenize:failed",
            PayPalNativeCheckoutAnalytics.TOKENIZATION_FAILED.event
        )
        assertEquals(
            "paypal-native:tokenize:succeeded",
            PayPalNativeCheckoutAnalytics.TOKENIZATION_SUCCEEDED.event
        )
        assertEquals(
            "paypal-native:tokenize:canceled",
            PayPalNativeCheckoutAnalytics.TOKENIZATION_CANCELED.event
        )
        assertEquals(
            "paypal-native:tokenize:order-creation:failed",
            PayPalNativeCheckoutAnalytics.ORDER_CREATION_FAILED.event
        )
    }
}
