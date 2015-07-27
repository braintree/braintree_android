package com.braintreepayments.api.models;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.api.BraintreeTestUtils.getConfigurationFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ConfigurationTest {

    @Test(timeout = 1000, expected = JSONException.class)
    @SmallTest
    public void fromJson_throwsForEmptyString() throws JSONException {
        Configuration.fromJson("");
    }

    @Test(timeout = 1000, expected = JSONException.class)
    @SmallTest
    public void fromJson_throwsForRandomJson() throws JSONException {
        getConfigurationFromFixture(getTargetContext(), "random_json.json");
    }

    @Test(timeout = 1000, expected = JSONException.class)
    @SmallTest
    public void fromJson_throwsWhenNoClientApiUrlPresent() throws JSONException {
        getConfigurationFromFixture(getTargetContext(), "configuration_without_client_api_url.json");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void fromJson_parsesClientApiUrlFromToken() throws JSONException {
        Configuration configuration = getConfigurationFromFixture(getTargetContext(),
                "configuration_with_client_api_url.json");

        assertEquals("client_api_url", configuration.getClientApiUrl());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void fromJson_handlesAbsentChallenges() throws JSONException {
        Configuration configuration = getConfigurationFromFixture(getTargetContext(),
                "configuration_without_challenge.json");

        assertFalse(configuration.isCvvChallengePresent());
        assertFalse(configuration.isPostalCodeChallengePresent());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void fromJson_parsesSingleChallengeFromToken() throws JSONException {
        Configuration configuration = getConfigurationFromFixture(getTargetContext(),
                "configuration_with_cvv_challenge.json");

        assertTrue(configuration.isCvvChallengePresent());
        assertFalse(configuration.isPostalCodeChallengePresent());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void fromJson_parsesAllChallengesFromToken() throws JSONException {
        Configuration configuration = getConfigurationFromFixture(getTargetContext(),
                "configuration_with_multiple_challenges.json");

        assertTrue(configuration.isCvvChallengePresent());
        assertTrue(configuration.isPostalCodeChallengePresent());
    }

    @Test(timeout = 1000, expected = JSONException.class)
    @SmallTest
    public void fromJson_throwsWhenNoMerchantIdPresent() throws JSONException {
        getConfigurationFromFixture(getTargetContext(), "configuration_without_merchant_id.json");
    }

    @Test(timeout = 1000)
    @SmallTest
    public void fromJson_parsesMerchantIdFromToken() throws JSONException {
        Configuration configuration = getConfigurationFromFixture(getTargetContext(),
                "configuration_with_merchant_id.json");

        assertEquals("integration_merchant_id", configuration.getMerchantId());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void fromJson_parsesMerchantAccountIdFromToken() throws JSONException {
        Configuration configuration = getConfigurationFromFixture(getTargetContext(),
                "configuration_with_merchant_account_id.json");

        assertEquals("integration_merchant_account_id", configuration.getMerchantAccountId());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void returnsOffIfVenmoIsNull() throws JSONException {
        Configuration configuration = getConfigurationFromFixture(getTargetContext(),
                "configuration_with_null_venmo.json");

        assertEquals("off", configuration.getVenmoState());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void returnsVenmoStatus() throws JSONException {
        Configuration configuration = getConfigurationFromFixture(getTargetContext(),
                "configuration_with_offline_venmo.json");

        assertEquals("offline", configuration.getVenmoState());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void reportsThreeDSecureEnabledWhenEnabled() throws JSONException {
        Configuration configuration = getConfigurationFromFixture(getTargetContext(),
                "configuration_with_three_d_secure.json");

        assertTrue(configuration.isThreeDSecureEnabled());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void reportsThreeDSecureDisabledWhenAbsent() throws JSONException {
        Configuration configuration = getConfigurationFromFixture(getTargetContext(),
                "configuration_with_no_three_d_secure.json");

        assertFalse(configuration.isThreeDSecureEnabled());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void returnsNewAndroidPayConfigurationWhenAndroidPayIsNull() throws JSONException {
        Configuration configuration = getConfigurationFromFixture(getTargetContext(),
                "configuration_without_android_pay.json");

        assertNotNull(configuration.getAndroidPay());
        assertFalse(configuration.getAndroidPay().isEnabled());
        assertNull(configuration.getAndroidPay().getDisplayName());
        assertNull(configuration.getAndroidPay().getEnvironment());
    }
}
