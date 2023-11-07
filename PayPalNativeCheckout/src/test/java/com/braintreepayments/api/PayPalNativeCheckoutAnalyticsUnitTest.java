package com.braintreepayments.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PayPalNativeCheckoutAnalyticsUnitTest {

    @Test
    public void testAnalyticsEvents_sendsExpectedEventNames() {
        assertEquals("paypal-native:tokenize:started",
                PayPalNativeCheckoutAnalytics.TOKENIZATION_STARTED.getEvent());
        assertEquals("paypal-native:tokenize:failed",
                PayPalNativeCheckoutAnalytics.TOKENIZATION_FAILED.getEvent());
        assertEquals("paypal-native:tokenize:succeeded",
                PayPalNativeCheckoutAnalytics.TOKENIZATION_SUCCEEDED.getEvent());
        assertEquals("paypal-native:tokenize:canceled",
                PayPalNativeCheckoutAnalytics.TOKENIZATION_CANCELED.getEvent());
        assertEquals("paypal-native:tokenize:order-creation:failed",
                PayPalNativeCheckoutAnalytics.ORDER_CREATION_FAILED.getEvent());
    }
}
