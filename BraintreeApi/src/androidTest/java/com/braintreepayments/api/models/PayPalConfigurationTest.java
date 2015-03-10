package com.braintreepayments.api.models;

import android.test.AndroidTestCase;

import com.braintreepayments.testutils.FixturesHelper;

public class PayPalConfigurationTest extends AndroidTestCase {

    public void testParsesPayPalConfigurationFromToken() {
        Configuration configuration = getConfiguration("configuration_with_offline_paypal.json");

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
        Configuration configuration = getConfiguration("configuration_with_disabled_paypal.json");

        assertFalse(configuration.isPayPalEnabled());
    }

    public void testReportsPayPalNotEnabledWhenConfigAbsent() {
        Configuration configuration = getConfiguration("configuration_with_disabled_paypal.json");

        assertFalse(configuration.isPayPalEnabled());
    }

    public void testExposesPayPalTouchKillSwitch() {
        Configuration configuration = getConfiguration("configuration_with_paypal_touch_disabled.json");

        assertTrue(configuration.getPayPal().getTouchDisabled());
    }

    /* helpers */
    private Configuration getConfiguration(String fixture) {
        return Configuration.fromJson(FixturesHelper.stringFromFixture(getContext(), fixture));
    }
}
