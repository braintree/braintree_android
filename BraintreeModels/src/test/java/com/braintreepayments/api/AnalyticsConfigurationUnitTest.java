package com.braintreepayments.api;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class AnalyticsConfigurationUnitTest {

    @Test
    public void parsesAnalyticsConfigurationFromToken() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);

        assertTrue(configuration.getAnalytics().isEnabled());
        assertEquals("analytics_url", configuration.getAnalytics().getUrl());
    }

    @Test
    public void fromJson_parsesUrl() throws JSONException {
        JSONObject json = new JSONObject().put("url", "analytics_url");

        assertEquals("analytics_url", AnalyticsConfiguration.fromJson(json).getUrl());
    }

    @Test
    public void fromJson_returnsNewAnalyticsConfigurationWhenJSONObjectIsNull() {
        AnalyticsConfiguration analyticsConfiguration = AnalyticsConfiguration.fromJson(null);

        assertNotNull(analyticsConfiguration);
        assertNull(analyticsConfiguration.getUrl());
    }

    @Test
    public void fromJson_returnsNewAnalyticsConfigurationWithoutUrlWhenUrlNotPresent() {
        AnalyticsConfiguration analyticsConfiguration = AnalyticsConfiguration.fromJson(new JSONObject());

        assertNotNull(analyticsConfiguration);
        assertNull(analyticsConfiguration.getUrl());
    }

    @Test
    public void toJson_serializesUrl() throws JSONException {
        JSONObject json = new JSONObject().put("url", "analytics_url");
        AnalyticsConfiguration analyticsConfiguration = AnalyticsConfiguration.fromJson(json);

        JSONObject jsonAnalyticsConfiguration = analyticsConfiguration.toJson();

        assertEquals("analytics_url", jsonAnalyticsConfiguration.getString("url"));
    }

    @Test
    public void toJson_serializesWhenUrlNotPresent() {
        AnalyticsConfiguration analyticsConfiguration = AnalyticsConfiguration.fromJson(new JSONObject());

        JSONObject jsonAnalyticsConfiguration = analyticsConfiguration.toJson();

        assertEquals("{}", jsonAnalyticsConfiguration.toString());
    }

    @Test
    public void reportsAnalyticsDisabledWhenNoAnalyticsPresent() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ANALYTICS);

        assertFalse(configuration.getAnalytics().isEnabled());
    }

    @Test
    public void reportsAnalyticsDisabledWhenUrlIsEmpty() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_EMPTY_ANALYTICS_URL);

        assertNotNull(configuration.getAnalytics());
        assertEquals("", configuration.getAnalytics().getUrl());
        assertFalse(configuration.getAnalytics().isEnabled());
    }
}
