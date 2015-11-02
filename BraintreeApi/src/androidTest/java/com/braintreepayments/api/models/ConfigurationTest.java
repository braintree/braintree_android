package com.braintreepayments.api.models;

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.SmallTest;
import android.text.TextUtils;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
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
        Configuration.fromJson(stringFromFixture("random_json.json"));
    }

    @Test(timeout = 1000, expected = JSONException.class)
    @SmallTest
    public void fromJson_throwsWhenNoClientApiUrlPresent() throws JSONException {
        Configuration.fromJson(stringFromFixture("configuration_without_client_api_url.json"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void fromJson_parsesClientApiUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_client_api_url.json"));

        assertEquals("client_api_url", configuration.getClientApiUrl());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void fromJson_handlesAbsentChallenges() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_without_challenge.json"));

        assertFalse(configuration.isCvvChallengePresent());
        assertFalse(configuration.isPostalCodeChallengePresent());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void fromJson_parsesSingleChallenge() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_cvv_challenge.json"));

        assertTrue(configuration.isCvvChallengePresent());
        assertFalse(configuration.isPostalCodeChallengePresent());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void fromJson_parsesAllChallenges() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_multiple_challenges.json"));

        assertTrue(configuration.isCvvChallengePresent());
        assertTrue(configuration.isPostalCodeChallengePresent());
    }

    @Test(timeout = 1000, expected = JSONException.class)
    @SmallTest
    public void fromJson_throwsWhenNoMerchantIdPresent() throws JSONException {
        Configuration.fromJson(stringFromFixture("configuration_without_merchant_id.json"));
    }

    @Test(timeout = 1000, expected = JSONException.class)
    @SmallTest
    public void fromJson_throwsWhenNoEnvironmentPresent() throws JSONException {
        Configuration.fromJson(stringFromFixture("configuration_without_environment.json"));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void fromJson_parsesEnvironment() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_environment.json"));

        assertEquals("integration_merchant_id", configuration.getMerchantId());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void fromJson_parsesMerchantId() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_merchant_id.json"));

        assertEquals("integration_merchant_id", configuration.getMerchantId());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void fromJson_parsesMerchantAccountId() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_merchant_account_id.json"));

        assertEquals("integration_merchant_account_id", configuration.getMerchantAccountId());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void returnsEmptyVenmoConfigurationWhenNotDefined() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration.json"));

        assertNotNull(configuration.getPayWithVenmo());
        assertTrue(TextUtils.isEmpty(configuration.getPayWithVenmo().getAccessToken()));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void payWithVenmoIsEnabledWhenConfigurationExists() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_pay_with_venmo.json"));

        assertNotNull(configuration.getPayWithVenmo());
        assertFalse(TextUtils.isEmpty(configuration.getPayWithVenmo().getAccessToken()));
    }

    @Test(timeout = 1000)
    @SmallTest
    public void reportsThreeDSecureEnabledWhenEnabled() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_three_d_secure.json"));

        assertTrue(configuration.isThreeDSecureEnabled());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void reportsThreeDSecureDisabledWhenAbsent() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_with_no_three_d_secure.json"));

        assertFalse(configuration.isThreeDSecureEnabled());
    }

    @Test(timeout = 1000)
    @SmallTest
    public void returnsNewAndroidPayConfigurationWhenAndroidPayIsNull() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration_without_android_pay.json"));

        assertNotNull(configuration.getAndroidPay());
        assertFalse(configuration.getAndroidPay().isEnabled(getTargetContext()));
        assertNull(configuration.getAndroidPay().getDisplayName());
        assertNull(configuration.getAndroidPay().getEnvironment());
    }
}
