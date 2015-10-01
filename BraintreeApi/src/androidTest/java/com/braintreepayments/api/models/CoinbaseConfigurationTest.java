package com.braintreepayments.api.models;

import android.test.AndroidTestCase;

import static com.braintreepayments.api.TestUtils.getConfigurationFromFixture;

public class CoinbaseConfigurationTest extends AndroidTestCase {

    public void testParsesCoinbaseConfiguration() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_with_coinbase.json");

        assertTrue(configuration.isCoinbaseEnabled());

        CoinbaseConfiguration coinbaseConfiguration = configuration.getCoinbase();

        assertEquals("coinbase-client-id", coinbaseConfiguration.getClientId());
        assertEquals("coinbase-merchant@example.com", coinbaseConfiguration.getMerchantAccount());
        assertEquals("authorizations:test user", coinbaseConfiguration.getScopes());
    }

    public void testReportsCoinbaseNotEnabledWhenFlagged() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_with_disabled_coinbase.json");

        assertFalse(configuration.isCoinbaseEnabled());
    }

    public void testReportsCoinbaseNotEnabledWhenConfigAbsent() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_with_missing_coinbase.json");

        assertFalse(configuration.isCoinbaseEnabled());
    }
}
