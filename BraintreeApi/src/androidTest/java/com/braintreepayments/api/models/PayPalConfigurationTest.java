package com.braintreepayments.api.models;

import android.test.AndroidTestCase;

import static com.braintreepayments.api.TestUtils.getConfigurationFromFixture;

public class PayPalConfigurationTest extends AndroidTestCase {

    public void testParsesPayPalConfiguration() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_with_offline_paypal.json");

        assertTrue(configuration.isPayPalEnabled());

        PayPalConfiguration payPalConfiguration = configuration.getPayPal();

        assertEquals("paypal_merchant", payPalConfiguration.getDisplayName());
        assertEquals("paypal_client_id", payPalConfiguration.getClientId());
        assertEquals("http://www.example.com/privacy", payPalConfiguration.getPrivacyUrl());
        assertEquals("http://www.example.com/user_agreement", payPalConfiguration.getUserAgreementUrl());
        assertEquals("http://localhost:9000/v1/", payPalConfiguration.getDirectBaseUrl());
        assertEquals("offline", payPalConfiguration.getEnvironment());
    }

    public void testReportsPayPalNotEnabledWhenFlagged() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_with_disabled_paypal.json");

        assertFalse(configuration.isPayPalEnabled());
    }

    public void testReportsPayPalNotEnabledWhenConfigAbsent() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_with_missing_paypal.json");

        assertFalse(configuration.isPayPalEnabled());
    }

    public void testExposesPayPalTouchKillSwitch() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_with_paypal_touch_disabled.json");

        assertTrue(configuration.getPayPal().getTouchDisabled());
    }
}
