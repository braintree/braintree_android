package com.braintreepayments.api

import android.text.TextUtils
import org.json.JSONException
import org.junit.Assert.*
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.*

@RunWith(RobolectricTestRunner::class)
class ConfigurationUnitTest {

    @Test(expected = JSONException::class)
    fun fromJson_throwsForNull() {
        Configuration.fromJson(null)
    }

    @Test(expected = JSONException::class)
    fun fromJson_throwsForEmptyString() {
        Configuration.fromJson("")
    }

    @Test(expected = JSONException::class)
    fun fromJson_throwsForRandomJson() {
        Configuration.fromJson(Fixtures.RANDOM_JSON)
    }

    @Test(expected = JSONException::class)
    fun fromJson_throwsWhenNoClientApiUrlPresent() {
        Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_CLIENT_API_URL)
    }

    @Test
    fun fromJson_parsesClientApiUrl() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_CLIENT_API_URL)
        assertEquals("client_api_url", sut.clientApiUrl)
    }

    @Test
    fun fromJson_parsesAssetsUrl() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ASSETS_URL)
        assertEquals("https://assets.braintreegateway.com", sut.assetsUrl)
    }

    @Test
    fun fromJson_parsesCardinalAuthenticationJwt() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_CARDINAL_AUTHENTICATION_JWT)
        assertEquals("cardinal_authentication_jwt", sut.cardinalAuthenticationJwt)
    }

    @Test
    fun fromJson_handlesAbsentChallenges() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_CHALLENGE)
        assertFalse(sut.isCvvChallengePresent)
        assertFalse(sut.isPostalCodeChallengePresent)
    }

    @Test
    fun fromJson_parsesSingleChallenge() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_CVV_CHALLENGE)
        assertTrue(sut.isCvvChallengePresent)
        assertFalse(sut.isPostalCodeChallengePresent)
    }

    @Test
    fun fromJson_parsesAllChallenges() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_MULTIPLE_CHALLENGES)
        assertTrue(sut.isCvvChallengePresent)
        assertTrue(sut.isPostalCodeChallengePresent)
    }

    @Test(expected = JSONException::class)
    fun fromJson_throwsWhenNoMerchantIdPresent() {
        Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_MERCHANT_ID)
    }

    @Test(expected = JSONException::class)
    fun fromJson_throwsWhenNoEnvironmentPresent() {
        Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ENVIRONMENT)
    }

    @Test
    fun fromJson_parsesEnvironment() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
        assertEquals("integration_merchant_id", sut.merchantId)
    }

    @Test
    fun fromJson_parsesMerchantId() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_MERCHANT_ID)
        assertEquals("integration_merchant_id", sut.merchantId)
    }

    @Test
    fun fromJson_parsesMerchantAccountId() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_MERCHANT_ACCOUNT_ID)
        assertEquals("integration_merchant_account_id", sut.merchantAccountId)
    }

    @Test
    fun returnsEmptyVenmoConfigurationWhenNotDefined() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        assertTrue(TextUtils.isEmpty(sut.venmoAccessToken))
    }

    @Test
    fun payWithVenmoIsEnabledWhenConfigurationExists() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO)
        assertFalse(TextUtils.isEmpty(sut.venmoAccessToken))
    }

    @Test
    fun reportsThreeDSecureEnabledWhenEnabled() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_THREE_D_SECURE)
        assertTrue(sut.isThreeDSecureEnabled)
    }

    @Test
    fun reportsThreeDSecureDisabledWhenAbsent() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_THREE_D_SECURE)
        assertFalse(sut.isThreeDSecureEnabled)
    }

    @Test
    fun returnsNewGooglePayConfigurationWhenGooglePayIsNull() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_GOOGLE_PAY)
        assertFalse(sut.isGooglePayEnabled)
        assertEquals("", sut.googlePayDisplayName)
        assertNull(sut.googlePayEnvironment)
    }

    @Test
    fun returnsNewUnionPayConfigurationWhenUnionPayIsAbsent() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        assertFalse(sut.isUnionPayEnabled)
    }

    @Test
    fun returnsNewCardConfigurationWhenCardConfigurationIsAbsent() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        assertEquals(0, sut.supportedCardTypes.size)
    }

    @Test
    fun returnsVisaCheckoutConfiguration_whenVisaCheckoutConfigurationIsPresent() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_VISA_CHECKOUT)
        assertTrue(sut.isVisaCheckoutEnabled)
    }

    @Test
    fun returnsNewVisaCheckoutConfigurationWhenVisaCheckoutConfigurationIsAbsent() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        assertFalse(sut.isVisaCheckoutEnabled)
    }

    @Test
    fun returnsBraintreeApiConfigurationWhenBraintreeApiConfigurationPresent() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN)
        assertTrue(sut.isBraintreeApiEnabled)
    }

    @Test
    fun returnsNewBraintreeApiConfigurationWhenBraintreeApiConfigurationAbsent() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        assertFalse(sut.isBraintreeApiEnabled)
    }

    @Test
    fun returnsGraphQLConfiguration_whenGraphQLConfigurationIsPresent() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL)
        assertTrue(sut.isGraphQLEnabled)
    }

    @Test
    fun returnsNewGraphQLConfigurationWhenGraphQLConfigurationIsAbsent() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        assertFalse(sut.isGraphQLEnabled)
    }

    @Test
    fun isFraudDataCollectionEnabled_whenCardFraudDataCollectionEnabled_returnsTrue() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_CARD_COLLECT_DEVICE_DATA)
        assertTrue(sut.isFraudDataCollectionEnabled)
    }

    @Test
    fun supportedCardTypes_forwardsValuesFromConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_CARD_COLLECT_DEVICE_DATA)
        assertEquals(5, sut.supportedCardTypes.size)
        assertTrue(sut.supportedCardTypes.contains("American Express"))
        assertTrue(sut.supportedCardTypes.contains("Discover"))
        assertTrue(sut.supportedCardTypes.contains("JCB"))
        assertTrue(sut.supportedCardTypes.contains("MasterCard"))
        assertTrue(sut.supportedCardTypes.contains("Visa"))
    }

    @Test
    fun isVenmoEnabled_whenVenmoAccessTokenValid_returnsTrue() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO)
        assertTrue(sut.isVenmoEnabled)
    }

    @Test
    fun venmoAccessToken_forwardsAccessTokenFromVenmoConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO)
        assertEquals("access-token", sut.venmoAccessToken)
    }

    @Test
    fun venmoAccessToken_forwardsMerchantIdFromVenmoConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO)
        assertEquals("merchant-id", sut.venmoMerchantId)
    }

    @Test
    fun venmoAccessToken_forwardsEnvironmentFromVenmoConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO)
        assertEquals("environment", sut.venmoEnvironment)
    }

    @Test
    fun isGraphQLEnabled_forwardsInvocationToGraphQLConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL)
        assertTrue(sut.isGraphQLEnabled)
    }

    @Test
    fun isUnionPayEnabled_forwardsInvocationToUnionPayConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_UNIONPAY)
        assertTrue(sut.isUnionPayEnabled)
    }

    @Test
    fun isKountEnabled_alwaysReturnsFalse() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_KOUNT)
        assertFalse(sut.isKountEnabled)
    }

    @Test
    fun kountMerchantId_alwaysReturnsEmptyString() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_KOUNT)
        assertEquals("", sut.kountMerchantId)
    }

    @Test
    fun isLocalPaymentsEnabled_whenPayPalEnabled_returnsTrue() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        assertTrue(sut.isLocalPaymentEnabled)
    }

    @Test
    fun isVisaCheckoutEnabled_returnsFalseWhenConfigurationApiKeyDoesNotExist() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN)
        assertFalse(sut.isVisaCheckoutEnabled)
    }

    @Test
    fun payPalDisplayName_forwardsValueFromConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        assertEquals("paypal_merchant", sut.payPalDisplayName)
    }

    @Test
    fun payPalClientId_forwardsValueFromConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        assertEquals("paypal_client_id", sut.payPalClientId)
    }

    @Test
    fun payPalPrivacyUrl_forwardsValueFromConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        assertEquals("http://www.example.com/privacy", sut.payPalPrivacyUrl)
    }

    @Test
    fun payPalUserAgreementUrl_forwardsValueFromConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        assertEquals("http://www.example.com/user_agreement", sut.payPalUserAgreementUrl)
    }

    @Test
    fun payPalDirectBaseUrl_forwardsVersionedUrlFromConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        assertEquals("https://www.paypal.com/v1/", sut.payPalDirectBaseUrl)
    }

    @Test
    fun payPalEnvironment_forwardsValueFromConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        assertEquals("live", sut.payPalEnvironment)
    }

    @Test
    fun isPayPalTouchDisabled_forwardsValueFromConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        assertTrue(sut.isPayPalTouchDisabled)
    }

    @Test
    fun payPalCurrencyIsoCode_forwardsValueFromConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        assertEquals("USD", sut.payPalCurrencyIsoCode)
    }

    @Test
    fun isVisaCheckoutEnabled_returnsTrueWhenConfigurationApiKeyExists() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_VISA_CHECKOUT)
        assertTrue(sut.isVisaCheckoutEnabled)
    }

    @Test
    fun visaCheckoutSupportedNetworks_forwardsInvocationToVisaCheckoutConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_VISA_CHECKOUT)
        val expected = listOf("AMEX", "DISCOVER", "MASTERCARD", "VISA")
        assertEquals(expected, sut.visaCheckoutSupportedNetworks)
    }

    @Test
    fun visaCheckoutApiKey_forwardsInvocationToVisaCheckoutConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_VISA_CHECKOUT)
        assertEquals("gwApikey", sut.visaCheckoutApiKey)
    }

    @Test
    fun visaCheckoutExternalClientId_forwardsInvocationToVisaCheckoutConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_VISA_CHECKOUT)
        assertEquals("gwExternalClientId", sut.visaCheckoutExternalClientId)
    }

    @Test
    fun isGooglePayEnabled_whenGooglePayEnabledInConfig_returnsTrue() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY)
        assertTrue(sut.isGooglePayEnabled)
    }

    @Test
    fun googlePayAuthorizationFingerprint_forwardsValueFromConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY)
        assertEquals("google-auth-fingerprint", sut.googlePayAuthorizationFingerprint)
    }

    @Test
    fun googlePayEnvironment_forwardsValueFromConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY)
        assertEquals("sandbox", sut.googlePayEnvironment)
    }

    @Test
    fun googlePayDisplayName_forwardsValueFromConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY)
        assertEquals("Google Pay Merchant", sut.googlePayDisplayName)
    }

    @Test
    fun googlePaySupportedNetworks_forwardsValuesFromConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY)
        val expected = listOf("visa", "mastercard", "amex", "discover")
        assertEquals(expected, sut.googlePaySupportedNetworks)
    }

    @Test
    fun googlePayPayPalClientId_forwardsValuesFromConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY)
        assertEquals("pay-pal-client-id", sut.googlePayPayPalClientId)
    }

    @Ignore("Update Samsung Pay Configuration to only check configuration instead of checking " +
            "for `braintree-android-samsung-pay` classes to be present on Java classpath.")
    @Test
    @Throws(JSONException::class)
    fun isSamsungPayEnabled_returnsTrueWhenSamsungPayEnabledInConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_SAMSUNGPAY)
        assertTrue(sut.isSamsungPayEnabled)
    }

    @Test
    fun samsungPayMerchantDisplayName_forwardsValueFromConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_SAMSUNGPAY)
        assertEquals("some example merchant", sut.samsungPayMerchantDisplayName)
    }

    @Test
    fun samsungPayServiceId_forwardsValueFromConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_SAMSUNGPAY)
        assertEquals("some-service-id", sut.samsungPayServiceId)
    }

    @Test
    fun samsungPaySupportedCardBrands_forwardsValuesFromConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_SAMSUNGPAY)
        val expected = listOf("american_express", "discover", "jcb", "mastercard", "visa")
        assertEquals(expected, sut.samsungPaySupportedCardBrands)
    }

    @Test
    fun samsungPayAuthorization_forwardsValueFromConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_SAMSUNGPAY)
        assertEquals("example-samsung-authorization", sut.samsungPayAuthorization)
    }

    @Test
    fun samsungPayEnvironment_forwardsValueFromConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_SAMSUNGPAY)
        assertEquals("SANDBOX", sut.samsungPayEnvironment)
    }

    @Test
    fun isBraintreeApiEnabled_returnsTrueWhenAccessTokenPresent() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN)
        assertTrue(sut.isBraintreeApiEnabled)
    }

    @Test
    fun isBraintreeApiEnabled_returnsFalseWhenAccessTokenNotPresent() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        assertFalse(sut.isBraintreeApiEnabled)
    }

    @Test
    fun braintreeApiAccessToken_forwardsValueFromConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN)
        assertEquals("access-token-example", sut.braintreeApiAccessToken)
    }

    @Test
    fun braintreeApiUrl_forwardsValueFromConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN)
        assertEquals("https://braintree-api.com", sut.braintreeApiUrl)
    }

    @Test
    fun analyticsUrl_forwardsValueFromConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS)
        assertEquals("analytics_url", sut.analyticsUrl)
    }

    @Test
    fun isAnalyticsEnabled_returnsTrueWhenAnalyticsPresent() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ANALYTICS)
        assertTrue(sut.isAnalyticsEnabled)
    }

    @Test
    fun isAnalyticsEnabled_returnsFalseWhenAnalyticsNotPresent() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ANALYTICS)
        assertFalse(sut.isAnalyticsEnabled)
    }

    @Test
    fun isAnalyticsEnabled_returnsFalseWhenAnalyticsUrlEmpty() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_EMPTY_ANALYTICS_URL)
        assertFalse(sut.isAnalyticsEnabled)
    }

    @Test
    fun isGraphQLFeatureEnabled_returnsTrue_whenFeatureEnabled() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL)
        assertTrue(sut.isGraphQLFeatureEnabled(GraphQLConstants.Features.TOKENIZE_CREDIT_CARDS))
    }

    @Test
    fun isGraphQLFeatureEnabled_returnsFalse_whenFeatureNotEnabled() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL)
        assertFalse(sut.isGraphQLFeatureEnabled("a_different_feature"))
    }

    @Test
    fun graphQLUrl_forwardsValueFromConfiguration() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL)
        assertEquals("https://example-graphql.com/graphql", sut.graphQLUrl)
    }
}
