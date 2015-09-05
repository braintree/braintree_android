package com.braintreepayments.api.models;

import android.test.AndroidTestCase;

import static com.braintreepayments.api.TestUtils.getConfigurationFromFixture;

public class AnalyticsConfigurationTest extends AndroidTestCase {

    public void testParsesAnalyticsConfigurationFromToken() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_with_analytics.json");

        assertTrue(configuration.isAnalyticsEnabled());
        assertEquals("analytics_url", configuration.getAnalytics().getUrl());
    }

    public void testReportsAnalyticsDisabledWhenNoAnalyticsPresent() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                "configuration_without_analytics.json");

        assertNull(configuration.getAnalytics());
        assertFalse(configuration.isAnalyticsEnabled());
    }

    public void testReportsAnalyticsDisabledWhenUrlIsEmpty() {
        Configuration configuration = getConfigurationFromFixture(getContext(),
                        "configuration_with_empty_analytics_url.json");

        assertNotNull(configuration.getAnalytics());
        assertEquals("", configuration.getAnalytics().getUrl());
        assertFalse(configuration.isAnalyticsEnabled());
    }
}
