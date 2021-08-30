package com.braintreepayments.api;

import android.text.TextUtils;

import org.json.JSONException;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Arrays;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
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
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_CLIENT_API_URL);

        assertEquals("client_api_url", sut.getClientApiUrl());
    }

    @Test
    public void fromJson_parsesAssetsUrl() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ASSETS_URL);

        assertEquals("https://assets.braintreegateway.com", sut.getAssetsUrl());
    }

    @Test
    public void fromJson_parsesCardinalAuthenticationJwt() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_CARDINAL_AUTHENTICATION_JWT);

        assertEquals("cardinal_authentication_jwt", sut.getCardinalAuthenticationJwt());
    }

    @Test
    public void fromJson_handlesAbsentChallenges() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_CHALLENGE);

        assertFalse(sut.isCvvChallengePresent());
        assertFalse(sut.isPostalCodeChallengePresent());
    }

    @Test
    public void fromJson_parsesSingleChallenge() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_CVV_CHALLENGE);

        assertTrue(sut.isCvvChallengePresent());
        assertFalse(sut.isPostalCodeChallengePresent());
    }

    @Test
    public void fromJson_parsesAllChallenges() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_MULTIPLE_CHALLENGES);

        assertTrue(sut.isCvvChallengePresent());
        assertTrue(sut.isPostalCodeChallengePresent());
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
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT);
        assertEquals("integration_merchant_id", sut.getMerchantId());
    }

    @Test
    public void fromJson_parsesMerchantId() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_MERCHANT_ID);
        assertEquals("integration_merchant_id", sut.getMerchantId());
    }

    @Test
    public void fromJson_parsesMerchantAccountId() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_MERCHANT_ACCOUNT_ID);
        assertEquals("integration_merchant_account_id", sut.getMerchantAccountId());
    }

    @Test
    public void returnsEmptyVenmoConfigurationWhenNotDefined() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);
        assertTrue(TextUtils.isEmpty(sut.getVenmoAccessToken()));
    }

    @Test
    public void payWithVenmoIsEnabledWhenConfigurationExists() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO);
        assertFalse(TextUtils.isEmpty(sut.getVenmoAccessToken()));
    }

    @Test
    public void reportsThreeDSecureEnabledWhenEnabled() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_THREE_D_SECURE);
        assertTrue(sut.isThreeDSecureEnabled());
    }

    @Test
    public void reportsThreeDSecureDisabledWhenAbsent() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_THREE_D_SECURE);
        assertFalse(sut.isThreeDSecureEnabled());
    }

    @Test
    public void returnsNewGooglePayConfigurationWhenGooglePayIsNull() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_GOOGLE_PAY);

        assertFalse(sut.isGooglePayEnabled());
        assertEquals("", sut.getGooglePayDisplayName());
        assertNull(sut.getGooglePayEnvironment());
    }

    @Test
    public void returnsNewUnionPayConfigurationWhenUnionPayIsAbsent() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);
        assertFalse(sut.isUnionPayEnabled());
    }

    @Test
    public void returnsNewKountConfigurationWhenKountIsAbsent() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);
        assertFalse(sut.isKountEnabled());
    }

    @Test
    public void returnsNewCardConfigurationWhenCardConfigurationIsAbsent() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);
        assertEquals(0, sut.getSupportedCardTypes().size());
    }

    @Test
    public void returnsVisaCheckoutConfiguration_whenVisaCheckoutConfigurationIsPresent() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_VISA_CHECKOUT);
        assertTrue(sut.isVisaCheckoutEnabled());
    }

    @Test
    public void returnsNewVisaCheckoutConfigurationWhenVisaCheckoutConfigurationIsAbsent() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);
        assertFalse(sut.isVisaCheckoutEnabled());
    }

    @Test
    public void returnsBraintreeApiConfigurationWhenBraintreeApiConfigurationPresent() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN);
        assertTrue(sut.isBraintreeApiEnabled());
    }

    @Test
    public void returnsNewBraintreeApiConfigurationWhenBraintreeApiConfigurationAbsent() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);
        assertFalse(sut.isBraintreeApiEnabled());
    }

    @Test
    public void returnsGraphQLConfiguration_whenGraphQLConfigurationIsPresent() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL);
        assertTrue(sut.isGraphQLEnabled());
    }

    @Test
    public void returnsNewGraphQLConfigurationWhenGraphQLConfigurationIsAbsent() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);
        assertFalse(sut.isGraphQLEnabled());
    }

    @Test
    public void isFraudDataCollectionEnabled_whenCardFraudDataCollectionEnabled_returnsTrue() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_CARD_COLLECT_DEVICE_DATA);
        assertTrue(sut.isFraudDataCollectionEnabled());
    }

    @Test
    public void getSupportedCardTypes_forwardsValuesFromConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_CARD_COLLECT_DEVICE_DATA);
        assertEquals(5, sut.getSupportedCardTypes().size());
        assertTrue(sut.getSupportedCardTypes().contains("American Express"));
        assertTrue(sut.getSupportedCardTypes().contains("Discover"));
        assertTrue(sut.getSupportedCardTypes().contains("JCB"));
        assertTrue(sut.getSupportedCardTypes().contains("MasterCard"));
        assertTrue(sut.getSupportedCardTypes().contains("Visa"));
    }

    @Test
    public void isVenmoEnabled_whenVenmoAccessTokenValid_returnsTrue() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO);
        assertTrue(sut.isVenmoEnabled());
    }

    @Test
    public void getVenmoAccessToken_forwardsAccessTokenFromVenmoConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO);
        assertEquals("access-token", sut.getVenmoAccessToken());
    }

    @Test
    public void getVenmoAccessToken_forwardsMerchantIdFromVenmoConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO);
        assertEquals("merchant-id", sut.getVenmoMerchantId());
    }

    @Test
    public void getVenmoAccessToken_forwardsEnvironmentFromVenmoConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO);
        assertEquals("environment", sut.getVenmoEnvironment());
    }

    @Test
    public void isGraphQLEnabled_forwardsInvocationToGraphQLConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL);
        assertTrue(sut.isGraphQLEnabled());
    }

    @Test
    public void isUnionPayEnabled_forwardsInvocationToUnionPayConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_UNIONPAY);
        assertTrue(sut.isUnionPayEnabled());
    }

    @Test
    public void isKountEnabled_forwardsInvocationToKountConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_KOUNT);
        assertTrue(sut.isKountEnabled());
    }

    @Test
    public void getKountMerchantId_forwardsInvocationToKountConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_KOUNT);
        assertEquals("600000", sut.getKountMerchantId());
    }

    @Test
    public void isLocalPaymentsEnabled_whenPayPalEnabled_returnsTrue() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        assertTrue(sut.isLocalPaymentEnabled());
    }

    @Test
    public void isVisaCheckoutEnabled_returnsFalseWhenConfigurationApiKeyDoesNotExist() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN);
        assertFalse(sut.isVisaCheckoutEnabled());
    }

    @Test
    public void getPayPalDisplayName_forwardsValueFromConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        assertEquals("paypal_merchant", sut.getPayPalDisplayName());
    }

    @Test
    public void getPayPalClientId_forwardsValueFromConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        assertEquals("paypal_client_id", sut.getPayPalClientId());
    }

    @Test
    public void getPayPalPrivacyUrl_forwardsValueFromConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        assertEquals("http://www.example.com/privacy", sut.getPayPalPrivacyUrl());
    }

    @Test
    public void getPayPalUserAgreementUrl_forwardsValueFromConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        assertEquals("http://www.example.com/user_agreement", sut.getPayPalUserAgreementUrl());
    }

    @Test
    public void getPayPalDirectBaseUrl_forwardsVersionedUrlFromConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        assertEquals("https://www.paypal.com/v1/", sut.getPayPalDirectBaseUrl());
    }

    @Test
    public void getPayPalEnvironment_forwardsValueFromConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        assertEquals("live", sut.getPayPalEnvironment());
    }

    @Test
    public void isPayPalTouchDisabled_forwardsValueFromConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        assertTrue(sut.isPayPalTouchDisabled());
    }

    @Test
    public void getPayPalCurrencyIsoCode_forwardsValueFromConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL);
        assertEquals("USD", sut.getPayPalCurrencyIsoCode());
    }

    @Test
    public void isVisaCheckoutEnabled_returnsTrueWhenConfigurationApiKeyExists() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_VISA_CHECKOUT);
        assertTrue(sut.isVisaCheckoutEnabled());
    }

    @Test
    public void getVisaCheckoutSupportedNetworks_forwardsInvocationToVisaCheckoutConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_VISA_CHECKOUT);

        List<String> expected = Arrays.asList("AMEX", "DISCOVER", "MASTERCARD", "VISA");
        assertEquals(expected, sut.getVisaCheckoutSupportedNetworks());
    }

    @Test
    public void getVisaCheckoutApiKey_forwardsInvocationToVisaCheckoutConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_VISA_CHECKOUT);
        assertEquals("gwApikey", sut.getVisaCheckoutApiKey());
    }

    @Test
    public void getVisaCheckoutExternalClientId_forwardsInvocationToVisaCheckoutConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_VISA_CHECKOUT);
        assertEquals("gwExternalClientId", sut.getVisaCheckoutExternalClientId());
    }

    @Test
    public void isGooglePayEnabled_whenGooglePayEnabledInConfig_returnsTrue() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY);
        assertTrue(sut.isGooglePayEnabled());
    }

    @Test
    public void getGooglePayAuthorizationFingerprint_forwardsValueFromConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY);
        assertEquals("google-auth-fingerprint", sut.getGooglePayAuthorizationFingerprint());
    }

    @Test
    public void getGooglePayEnvironment_forwardsValueFromConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY);
        assertEquals("sandbox", sut.getGooglePayEnvironment());
    }

    @Test
    public void getGooglePayDisplayName_forwardsValueFromConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY);
        assertEquals("Google Pay Merchant", sut.getGooglePayDisplayName());
    }

    @Test
    public void getGooglePaySupportedNetworks_forwardsValuesFromConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY);

        List<String> expected = Arrays.asList("visa", "mastercard", "amex", "discover");
        assertEquals(expected, sut.getGooglePaySupportedNetworks());
    }

    @Test
    public void getGooglePayPayPalClientId_forwardsValuesFromConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY);
        assertEquals("pay-pal-client-id", sut.getGooglePayPayPalClientId());
    }

    @Test
    @Ignore("Update Samsung Pay Configuration to only check configuration instead of checking for `braintree-android-samsung-pay` classes to be present on Java classpath.")
    public void isSamsungPayEnabled_returnsTrueWhenSamsungPayEnabledInConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_SAMSUNGPAY);
        assertTrue(sut.isSamsungPayEnabled());
    }

    @Test
    public void getSamsungPayMerchantDisplayName_forwardsValueFromConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_SAMSUNGPAY);
        assertEquals("some example merchant", sut.getSamsungPayMerchantDisplayName());
    }

    @Test
    public void getSamsungPayServiceId_forwardsValueFromConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_SAMSUNGPAY);
        assertEquals("some-service-id", sut.getSamsungPayServiceId());
    }

    @Test
    public void getSamsungPaySupportedCardBrands_forwardsValuesFromConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_SAMSUNGPAY);

        List<String> expected = Arrays.asList("american_express", "discover", "jcb", "mastercard", "visa");
        assertEquals(expected, sut.getSamsungPaySupportedCardBrands());
    }

    @Test
    public void getSamsungPayAuthorization_forwardsValueFromConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_SAMSUNGPAY);
        assertEquals("example-samsung-authorization", sut.getSamsungPayAuthorization());
    }

    @Test
    public void getSamsungPayEnvironment_forwardsValueFromConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_SAMSUNGPAY);
        assertEquals("SANDBOX", sut.getSamsungPayEnvironment());
    }

    @Test
    public void isBraintreeApiEnabled_returnsTrueWhenAccessTokenPresent() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN);
        assertTrue(sut.isBraintreeApiEnabled());
    }

    @Test
    public void isBraintreeApiEnabled_returnsFalseWhenAccessTokenNotPresent() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN);
        assertFalse(sut.isBraintreeApiEnabled());
    }

    @Test
    public void getBraintreeApiAccessToken_forwardsValueFromConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN);
        assertEquals("access-token-example", sut.getBraintreeApiAccessToken());
    }

    @Test
    public void getBraintreeApiUrl_forwardsValueFromConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN);
        assertEquals("https://braintree-api.com", sut.getBraintreeApiUrl());
    }

    @Test
    public void getAnalyticsUrl_forwardsValueFromConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);
        assertEquals("analytics_url", sut.getAnalyticsUrl());
    }

    @Test
    public void isAnalyticsEnabled_returnsTrueWhenAnalyticsPresent() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS);
        assertTrue(sut.isAnalyticsEnabled());
    }

    @Test
    public void isAnalyticsEnabled_returnsFalseWhenAnalyticsNotPresent() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ANALYTICS);
        assertFalse(sut.isAnalyticsEnabled());
    }

    @Test
    public void isAnalyticsEnabled_returnsFalseWhenAnalyticsUrlEmpty() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_EMPTY_ANALYTICS_URL);
        assertFalse(sut.isAnalyticsEnabled());
    }

    @Test
    public void isGraphQLFeatureEnabled_returnsTrue_whenFeatureEnabled() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL);
        assertTrue(sut.isGraphQLFeatureEnabled(GraphQLConstants.Features.TOKENIZE_CREDIT_CARDS));
    }

    @Test
    public void isGraphQLFeatureEnabled_returnsFalse_whenFeatureNotEnabled() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL);
        assertFalse(sut.isGraphQLFeatureEnabled("a_different_feature"));
    }

    @Test
    public void getGraphQLUrl_forwardsValueFromConfiguration() throws JSONException {
        Configuration sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL);
        assertEquals("https://example-graphql.com/graphql", sut.getGraphQLUrl());
    }
}
