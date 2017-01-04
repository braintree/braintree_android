package com.braintreepayments.api.models;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.powermock.api.support.membermodification.MemberMatcher.method;
import static org.powermock.api.support.membermodification.MemberModifier.stub;

@RunWith(PowerMockRunner.class)
@PrepareForTest(VisaCheckoutConfiguration.class)
public class VisaCheckoutConfigurationUnitTest {

    private JSONObject mVisaCheckoutConfiguration;

    @Before
    public void setup() throws JSONException {
        JSONObject configuration = new JSONObject(stringFromFixture("configuration/with_visa_checkout.json"));
        mVisaCheckoutConfiguration = configuration.getJSONObject("visaCheckout");
    }

    @Test
    public void isEnabled_returnsFalseWhenConfigurationExistsButVisaPackageUnavailable() throws JSONException {
        stub(method(VisaCheckoutConfiguration.class, "isVisaPackageAvailable")).toReturn(false);

        VisaCheckoutConfiguration visaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(mVisaCheckoutConfiguration);

        assertFalse(visaCheckoutConfiguration.isEnabled());
    }

    @Test
    public void isEnabled_returnsTrueWhenConfigurationExists() throws JSONException {
        VisaCheckoutConfiguration visaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(mVisaCheckoutConfiguration);

        assertTrue(visaCheckoutConfiguration.isEnabled());
    }

    @Test
    public void getApiKey_returnsApiKeyWhenConfigurationExists() throws JSONException {
        VisaCheckoutConfiguration visaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(mVisaCheckoutConfiguration);

        assertEquals("gwApikey", visaCheckoutConfiguration.getApiKey());
    }

    @Test
    public void getExternalClientId_returnsExternalClientIdWhenConfigurationExists() throws JSONException {
        VisaCheckoutConfiguration visaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(mVisaCheckoutConfiguration);

        assertEquals("gwExternalClientId", visaCheckoutConfiguration.getExternalClientId());
    }

    @Test
    public void isEnabled_returnsFalseWhenConfigurationDoesntExist() {
        VisaCheckoutConfiguration visaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(null);

        assertFalse(visaCheckoutConfiguration.isEnabled());
    }

    @Test
    public void getApiKey_returnsEmptyStringWhenConfigurationDoesntExist() {
        VisaCheckoutConfiguration visaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(null);

        assertTrue(visaCheckoutConfiguration.getApiKey().isEmpty());
    }

    @Test
    public void getExternalClientId_returnsEmptyStringWhenConfigurationDoesntExist() {
        VisaCheckoutConfiguration visaCheckoutConfiguration = VisaCheckoutConfiguration.fromJson(null);

        assertTrue(visaCheckoutConfiguration.getExternalClientId().isEmpty());
    }
}
