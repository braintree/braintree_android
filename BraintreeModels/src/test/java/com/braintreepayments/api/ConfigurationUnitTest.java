package com.braintreepayments.api;

import android.text.TextUtils;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

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
        Configuration.fromJson(Fixtures.RANDOM_JSON);
    }

    @Test(expected = JSONException.class)
    public void fromJson_throwsWhenNoClientApiUrlPresent() throws JSONException {
        Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_CLIENT_API_URL);
    }

    @Test
    public void fromJson_parsesClientApiUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_CLIENT_API_URL);

        assertEquals("client_api_url", configuration.getClientApiUrl());
    }

    @Test
    public void fromJson_parsesAssetsUrl() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ASSETS_URL);

        assertEquals("https://assets.braintreegateway.com", configuration.getAssetsUrl());
    }

    @Test
    public void fromJson_parsesCardinalAuthenticationJwt() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_CARDINAL_AUTHENTICATION_JWT);

        assertEquals("cardinal_authentication_jwt", configuration.getCardinalAuthenticationJwt());
    }

    @Test
    public void fromJson_handlesAbsentChallenges() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_CHALLENGE);

        assertFalse(configuration.isCvvChallengePresent());
        assertFalse(configuration.isPostalCodeChallengePresent());
    }

    @Test
    public void fromJson_parsesSingleChallenge() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_CVV_CHALLENGE);

        assertTrue(configuration.isCvvChallengePresent());
        assertFalse(configuration.isPostalCodeChallengePresent());
    }

    @Test
    public void fromJson_parsesAllChallenges() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_MULTIPLE_CHALLENGES);

        assertTrue(configuration.isCvvChallengePresent());
        assertTrue(configuration.isPostalCodeChallengePresent());
    }

    @Test(expected = JSONException.class)
    public void fromJson_throwsWhenNoMerchantIdPresent() throws JSONException {
        Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_MERCHANT_ID);
    }

    @Test(expected = JSONException.class)
    public void fromJson_throwsWhenNoEnvironmentPresent() throws JSONException {
        Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ENVIRONMENT);
    }

    @Test
    public void fromJson_parsesEnvironment() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT);
        assertEquals("integration_merchant_id", configuration.getMerchantId());
    }

    @Test
    public void fromJson_parsesMerchantId() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_MERCHANT_ID);
        assertEquals("integration_merchant_id", configuration.getMerchantId());
    }

    @Test
    public void fromJson_parsesMerchantAccountId() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_MERCHANT_ACCOUNT_ID);
        assertEquals("integration_merchant_account_id", configuration.getMerchantAccountId());
    }

    @Test
    public void returnsEmptyVenmoConfigurationWhenNotDefined() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);

        assertNotNull(configuration.getPayWithVenmo());
        assertTrue(TextUtils.isEmpty(configuration.getPayWithVenmo().getAccessToken()));
    }

    @Test
    public void payWithVenmoIsEnabledWhenConfigurationExists() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO);

        assertNotNull(configuration.getPayWithVenmo());
        assertFalse(TextUtils.isEmpty(configuration.getPayWithVenmo().getAccessToken()));
    }

    @Test
    public void reportsThreeDSecureEnabledWhenEnabled() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_THREE_D_SECURE);

        assertTrue(configuration.isThreeDSecureEnabled());
    }

    @Test
    public void reportsThreeDSecureDisabledWhenAbsent() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_THREE_D_SECURE);

        assertFalse(configuration.isThreeDSecureEnabled());
    }

    @Test
    public void returnsNewGooglePayConfigurationWhenGooglePayIsNull() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_GOOGLE_PAY);

        assertNotNull(configuration.getGooglePay());
        assertFalse(configuration.getGooglePay().isEnabled());
        assertEquals("", configuration.getGooglePay().getDisplayName());
        assertNull(configuration.getGooglePay().getEnvironment());
    }

    @Test
    public void returnsNewUnionPayConfigurationWhenUnionPayIsAbsent() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);

        assertFalse(configuration.getUnionPay().isEnabled());
    }

    @Test
    public void returnsNewKountConfigurationWhenKountIsAbsent() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);

        assertNotNull(configuration.getKount());
        assertFalse(configuration.getKount().isEnabled());
    }

    @Test
    public void returnsNewCardConfigurationWhenCardConfigurationIsAbsent() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);

        assertNotNull(configuration.getCardConfiguration());
        assertEquals(0, configuration.getCardConfiguration().getSupportedCardTypes().size());
    }

    @Test
    public void returnsVisaCheckoutConfiguration_whenVisaCheckoutConfigurationIsPresent() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_VISA_CHECKOUT);

        assertTrue(configuration.getVisaCheckout().isEnabled());
    }

    @Test
    public void returnsNewVisaCheckoutConfigurationWhenVisaCheckoutConfigurationIsAbsent() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);

        assertFalse(configuration.getVisaCheckout().isEnabled());
    }

    @Test
    public void returnsBraintreeApiConfigurationWhenBraintreeApiConfigurationPresent() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN);

        assertNotNull(configuration.getBraintreeApiConfiguration());
        assertTrue(configuration.getBraintreeApiConfiguration().isEnabled());
    }

    @Test
    public void returnsNewBraintreeApiConfigurationWhenBraintreeApiConfigurationAbsent() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);

        assertNotNull(configuration.getBraintreeApiConfiguration());
        assertFalse(configuration.getBraintreeApiConfiguration().isEnabled());
    }

    @Test
    public void returnsGraphQLConfiguration_whenGraphQLConfigurationIsPresent() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL);

        assertTrue(configuration.getGraphQL().isEnabled());
    }

    @Test
    public void returnsSamsungPayConfiguration_whenSamsungPayConfigurationIsPresent() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_SAMSUNGPAY);

        assertNotNull(configuration.getSamsungPay());
        assertEquals("some-service-id", configuration.getSamsungPay().getServiceId());
    }

    @Test
    public void returnsNewGraphQLConfigurationWhenGraphQLConfigurationIsAbsent() throws JSONException {
        Configuration configuration = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);

        assertFalse(configuration.getGraphQL().isEnabled());
    }
}
