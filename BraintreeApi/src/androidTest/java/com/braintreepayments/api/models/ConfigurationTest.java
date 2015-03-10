package com.braintreepayments.api.models;

import android.test.AndroidTestCase;

import java.io.IOException;

import static com.braintreepayments.api.TestUtils.getConfigurationFromFixture;

public class ConfigurationTest extends AndroidTestCase {

    public void testParsesSingleChallengeFromToken() throws IOException {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_with_cvv_challenge.json");

        assertTrue(configuration.isCvvChallengePresent());
        assertFalse(configuration.isPostalCodeChallengePresent());
    }

    public void testParsesAllChallengesFromToken() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_with_multiple_challenges.json");

        assertTrue(configuration.isCvvChallengePresent());
        assertTrue(configuration.isPostalCodeChallengePresent());
    }

    public void testParsesMerchantIdFromToken() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_with_merchant_id.json");

        assertEquals("integration_merchant_id", configuration.getMerchantId());
    }

    public void testParsesMerchantAccountIdFromToken() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_with_merchant_account_id.json");

        assertEquals("integration_merchant_account_id", configuration.getMerchantAccountId());
    }

    public void testReturnsOffIfVenmoIsNull() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_with_null_venmo.json");

        assertEquals("off", configuration.getVenmoState());
    }

    public void testReturnsVenmoStatus() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_with_offline_venmo.json");

        assertEquals("offline", configuration.getVenmoState());
    }

    public void testReportsThreeDSecureEnabledWhenEnabled() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_with_three_d_secure.json");

        assertTrue(configuration.isThreeDSecureEnabled());
    }

    public void testReportsThreeDSecureDisabledWhenAbsent() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_with_no_three_d_secure.json");

        assertFalse(configuration.isThreeDSecureEnabled());
    }
}
