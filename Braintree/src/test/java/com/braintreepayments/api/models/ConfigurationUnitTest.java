package com.braintreepayments.api.models;

import android.text.TextUtils;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class ConfigurationUnitTest {

    @Test(expected = JSONException.class)
    public void fromJson_throwsForNull() throws JSONException {
        Configuration.fromJson(null);
    }

    @Test(expected = JSONException.class)
    public void fromJson_throwsForEmptyString() throws JSONException {
        Configuration.fromJson("");
    }

    @Test(expected = JSONException.class)
    public void fromJson_throwsForRandomJson() throws JSONException {
        Configuration.fromJson(stringFromFixture("random_json.json"));
    }

    @Test(expected = JSONException.class)
    public void fromJson_throwsWhenNoClientApiUrlPresent() throws JSONException {
        Configuration.fromJson(stringFromFixture("configuration/without_client_api_url.json"));
    }

    @Test
    public void fromJson_parsesClientApiUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration/with_client_api_url.json"));

        assertEquals("client_api_url", configuration.getClientApiUrl());
    }

    @Test
    public void fromJson_parsesAssetsUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration/with_assets_url.json"));

        assertEquals("https://assets.braintreegateway.com", configuration.getAssetsUrl());
    }

    @Test
    public void fromJson_handlesAbsentChallenges() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/without_challenge.json"));

        assertFalse(configuration.isCvvChallengePresent());
        assertFalse(configuration.isPostalCodeChallengePresent());
    }

    @Test
    public void fromJson_parsesSingleChallenge() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration/with_cvv_challenge.json"));

        assertTrue(configuration.isCvvChallengePresent());
        assertFalse(configuration.isPostalCodeChallengePresent());
    }

    @Test
    public void fromJson_parsesAllChallenges() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration/with_multiple_challenges.json"));

        assertTrue(configuration.isCvvChallengePresent());
        assertTrue(configuration.isPostalCodeChallengePresent());
    }

    @Test(expected = JSONException.class)
    public void fromJson_throwsWhenNoMerchantIdPresent() throws JSONException {
        Configuration.fromJson(stringFromFixture("configuration/without_merchant_id.json"));
    }

    @Test(expected = JSONException.class)
    public void fromJson_throwsWhenNoEnvironmentPresent() throws JSONException {
        Configuration.fromJson(stringFromFixture("configuration/without_environment.json"));
    }

    @Test
    public void fromJson_parsesEnvironment() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration/with_environment.json"));

        assertEquals("integration_merchant_id", configuration.getMerchantId());
    }

    @Test
    public void fromJson_parsesMerchantId() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/with_merchant_id.json"));

        assertEquals("integration_merchant_id", configuration.getMerchantId());
    }

    @Test
    public void fromJson_parsesMerchantAccountId() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration/with_merchant_account_id.json"));

        assertEquals("integration_merchant_account_id", configuration.getMerchantAccountId());
    }

    @Test
    public void returnsEmptyVenmoConfigurationWhenNotDefined() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration/configuration.json"));

        assertNotNull(configuration.getPayWithVenmo());
        assertTrue(TextUtils.isEmpty(configuration.getPayWithVenmo().getAccessToken()));
    }

    @Test
    public void payWithVenmoIsEnabledWhenConfigurationExists() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration/with_pay_with_venmo.json"));

        assertNotNull(configuration.getPayWithVenmo());
        assertFalse(TextUtils.isEmpty(configuration.getPayWithVenmo().getAccessToken()));
    }

    @Test
    public void reportsThreeDSecureEnabledWhenEnabled() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration/with_three_d_secure.json"));

        assertTrue(configuration.isThreeDSecureEnabled());
    }

    @Test
    public void reportsThreeDSecureDisabledWhenAbsent() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration/with_no_three_d_secure.json"));

        assertFalse(configuration.isThreeDSecureEnabled());
    }

    @Test
    public void returnsNewAndroidPayConfigurationWhenAndroidPayIsNull() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration/without_android_pay.json"));

        assertNotNull(configuration.getAndroidPay());
        assertFalse(configuration.getAndroidPay().isEnabled(null));
        assertEquals("", configuration.getAndroidPay().getDisplayName());
        assertNull(configuration.getAndroidPay().getEnvironment());
    }

    @Test
    public void returnsNewUnionPayConfigurationWhenUnionPayIsAbsent() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration/configuration.json"));

        assertFalse(configuration.getUnionPay().isEnabled());
    }

    @Test
    public void returnsNewKountConfigurationWhenKountIsAbsent() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/configuration.json"));

        assertNotNull(configuration.getKount());
        assertFalse(configuration.getKount().isEnabled());
    }

    @Test
    public void returnsNewCardConfigurationWhenCardConfigurationIsAbsent() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/configuration.json"));

        assertNotNull(configuration.getCardConfiguration());
        assertEquals(0, configuration.getCardConfiguration().getSupportedCardTypes().size());
    }

    @Test
    public void returnsVisaCheckoutConfiguration_whenVisaCheckoutConfigurationIsPresent() throws JSONException {
        Configuration configuration = Configuration.fromJson(
                stringFromFixture("configuration/with_visa_checkout.json"));

        assertTrue(configuration.getVisaCheckout().isEnabled());
    }

    @Test
    public void returnsNewVisaCheckoutConfigurationWhenVisaCheckoutConfigurationIsAbsent() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/configuration.json"));

        assertFalse(configuration.getVisaCheckout().isEnabled());
    }

    @Test
    public void returnsBraintreeApiConfigurationWhenBraintreeApiConfigurationPresent() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/with_braintree_api.json"));

        assertNotNull(configuration.getBraintreeApiConfiguration());
        assertTrue(configuration.getBraintreeApiConfiguration().isEnabled());
    }

    @Test
    public void returnsNewBraintreeApiConfigurationWhenBraintreeApiConfigurationAbsent() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/configuration.json"));

        assertNotNull(configuration.getBraintreeApiConfiguration());
        assertFalse(configuration.getBraintreeApiConfiguration().isEnabled());
    }

    @Test
    public void returnsNewIdealConfigurationWhenIdealConfigurationAbsent() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/configuration.json"));

        assertNotNull(configuration.getIdealConfiguration());
        assertFalse(configuration.getIdealConfiguration().isEnabled());
    }

    @Test
    public void returnsIdealConfigurationWhenIdealConfigurationPresent() throws JSONException {
        Configuration configuration = Configuration.fromJson(stringFromFixture("configuration/with_ideal.json"));

        assertNotNull(configuration.getIdealConfiguration());
        assertTrue(configuration.getIdealConfiguration().isEnabled());
    }
}
