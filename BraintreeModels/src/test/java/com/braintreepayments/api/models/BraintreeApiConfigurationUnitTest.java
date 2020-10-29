package com.braintreepayments.api.models;

import com.braintreepayments.testutils.Fixtures;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class BraintreeApiConfigurationUnitTest {

    private Configuration mConfiguration;

    @Before
    public void setup() throws JSONException {
        mConfiguration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN);
    }

    @Test
    public void isEnabled_returnsTrueWhenAccessTokenPresent() {
        assertTrue(mConfiguration.getBraintreeApiConfiguration().isEnabled());
    }

    @Test
    public void isEnabled_returnsFalseWhenAccessTokenNotPresent() throws JSONException {
        mConfiguration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);
        assertFalse(mConfiguration.getBraintreeApiConfiguration().isEnabled());
    }

    @Test
    public void parsesAccessToken() {
        assertEquals("access-token-example", mConfiguration.getBraintreeApiConfiguration().getAccessToken());
    }

    @Test
    public void parsesUrl() {
        assertEquals("https://braintree-api.com", mConfiguration.getBraintreeApiConfiguration().getUrl());
    }
}
