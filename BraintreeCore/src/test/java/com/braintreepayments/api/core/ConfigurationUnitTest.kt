package com.braintreepayments.api.core

import android.text.TextUtils
import com.braintreepayments.api.testutils.Fixtures
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Suppress("DEPRECATION")
class ConfigurationUnitTest {

    private val configurationLoader: ConfigurationLoader = mockk(relaxed = true)
    private val authorization: Authorization = mockk(relaxed = true)

    @Test(expected = JSONException::class)
    fun `when given an empty string, fromJson throws JSONException`() {
        Configuration.fromJson("")
    }

    @Test(expected = JSONException::class)
    fun `when given random json, fromJson throws JSONException`() {
        Configuration.fromJson(Fixtures.RANDOM_JSON)
    }

    @Test(expected = JSONException::class)
    fun `when client api url is absent, fromJson throws JSONException`() {
        Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_CLIENT_API_URL)
    }

    @Test
    fun `fromJson parses the client api url`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_CLIENT_API_URL)
        assertEquals("client_api_url", sut.clientApiUrl)
    }

    @Test
    fun `fromJson parses the assets url`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ASSETS_URL)
        assertEquals("https://assets.braintreegateway.com", sut.assetsUrl)
    }

    @Test
    fun `fromJson parses the cardinal authentication jwt`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_CARDINAL_AUTHENTICATION_JWT)
        assertEquals("cardinal_authentication_jwt", sut.cardinalAuthenticationJwt)
    }

    @Test
    fun `when challenges are absent, fromJson reports no cvv or postal code challenge`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_CHALLENGE)
        assertFalse(sut.isCvvChallengePresent)
        assertFalse(sut.isPostalCodeChallengePresent)
    }

    @Test
    fun `when only cvv challenge is configured, fromJson reports cvv challenge present and postal code absent`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_CVV_CHALLENGE)
        assertTrue(sut.isCvvChallengePresent)
        assertFalse(sut.isPostalCodeChallengePresent)
    }

    @Test
    fun `when multiple challenges are configured, fromJson reports both cvv and postal code challenges present`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_MULTIPLE_CHALLENGES)
        assertTrue(sut.isCvvChallengePresent)
        assertTrue(sut.isPostalCodeChallengePresent)
    }

    @Test(expected = JSONException::class)
    fun `when merchant id is absent, fromJson throws JSONException`() {
        Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_MERCHANT_ID)
    }

    @Test(expected = JSONException::class)
    fun `when environment is absent, fromJson throws JSONException`() {
        Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ENVIRONMENT)
    }

    @Test
    fun `fromJson parses the merchant id from a configuration with environment`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ENVIRONMENT)
        assertEquals("integration_merchant_id", sut.merchantId)
    }

    @Test
    fun `fromJson parses the merchant id`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_MERCHANT_ID)
        assertEquals("integration_merchant_id", sut.merchantId)
    }

    @Test
    fun `fromJson parses the merchant account id`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_MERCHANT_ACCOUNT_ID)
        assertEquals("integration_merchant_account_id", sut.merchantAccountId)
    }

    @Test
    fun `when venmo configuration is not defined, venmoAccessToken is empty`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        assertTrue(TextUtils.isEmpty(sut.venmoAccessToken))
    }

    @Test
    fun `when pay with venmo configuration exists, venmoAccessToken is not empty`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO)
        assertFalse(TextUtils.isEmpty(sut.venmoAccessToken))
    }

    @Test
    fun `when three d secure configuration is present, isThreeDSecureEnabled is true`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_THREE_D_SECURE)
        assertTrue(sut.isThreeDSecureEnabled)
    }

    @Test
    fun `when three d secure configuration is absent, isThreeDSecureEnabled is false`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_THREE_D_SECURE)
        assertFalse(sut.isThreeDSecureEnabled)
    }

    @Test
    fun `when google pay configuration is null, fromJson returns default google pay values`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_GOOGLE_PAY)
        assertFalse(sut.isGooglePayEnabled)
        assertEquals("", sut.googlePayDisplayName)
        assertNull(sut.googlePayEnvironment)
    }

    @Test
    fun `when card configuration is absent, fromJson returns no supported card types`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        assertEquals(0, sut.supportedCardTypes.size)
    }

    @Test
    fun `when visa checkout configuration is present, isVisaCheckoutEnabled is true`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_VISA_CHECKOUT)
        assertTrue(sut.isVisaCheckoutEnabled)
    }

    @Test
    fun `when visa checkout configuration is absent, isVisaCheckoutEnabled is false`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        assertFalse(sut.isVisaCheckoutEnabled)
    }

    @Test
    fun `when braintree api configuration is present, isBraintreeApiEnabled is true`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN)
        assertTrue(sut.isBraintreeApiEnabled)
    }

    @Test
    fun `when braintree api configuration is absent, isBraintreeApiEnabled is false`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        assertFalse(sut.isBraintreeApiEnabled)
    }

    @Test
    fun `when graphQL configuration is present, isGraphQLEnabled is true`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL)
        assertTrue(sut.isGraphQLEnabled)
    }

    @Test
    fun `when graphQL configuration is absent, isGraphQLEnabled is false`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        assertFalse(sut.isGraphQLEnabled)
    }

    @Test
    fun `when card fraud data collection is enabled, isFraudDataCollectionEnabled is true`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_CARD_COLLECT_DEVICE_DATA)
        assertTrue(sut.isFraudDataCollectionEnabled)
    }

    @Test
    fun `supportedCardTypes forwards values from the card configuration`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_CARD_COLLECT_DEVICE_DATA)
        assertEquals(5, sut.supportedCardTypes.size)
        assertTrue(sut.supportedCardTypes.contains("American Express"))
        assertTrue(sut.supportedCardTypes.contains("Discover"))
        assertTrue(sut.supportedCardTypes.contains("JCB"))
        assertTrue(sut.supportedCardTypes.contains("MasterCard"))
        assertTrue(sut.supportedCardTypes.contains("Visa"))
    }

    @Test
    fun `when venmo access token is valid, isVenmoEnabled is true`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO)
        assertTrue(sut.isVenmoEnabled)
    }

    @Test
    fun `venmoAccessToken forwards the access token from venmo configuration`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO)
        assertEquals("access-token", sut.venmoAccessToken)
    }

    @Test
    fun `venmoMerchantId forwards the merchant id from venmo configuration`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO)
        assertEquals("merchant-id", sut.venmoMerchantId)
    }

    @Test
    fun `venmoEnvironment forwards the environment from venmo configuration`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_PAY_WITH_VENMO)
        assertEquals("environment", sut.venmoEnvironment)
    }

    @Test
    fun `when graphQL configuration exists, isGraphQLEnabled is true`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL)
        assertTrue(sut.isGraphQLEnabled)
    }

    @Test
    fun `when paypal is enabled, isLocalPaymentEnabled is true`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        assertTrue(sut.isLocalPaymentEnabled)
    }

    @Test
    fun `when visa checkout api key does not exist, isVisaCheckoutEnabled is false`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN)
        assertFalse(sut.isVisaCheckoutEnabled)
    }

    @Test
    fun `payPalDisplayName forwards the value from configuration`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        assertEquals("paypal_merchant", sut.payPalDisplayName)
    }

    @Test
    fun `payPalClientId forwards the value from configuration`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        assertEquals("paypal_client_id", sut.payPalClientId)
    }

    @Test
    fun `payPalPrivacyUrl forwards the value from configuration`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        assertEquals("http://www.example.com/privacy", sut.payPalPrivacyUrl)
    }

    @Test
    fun `payPalUserAgreementUrl forwards the value from configuration`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        assertEquals("http://www.example.com/user_agreement", sut.payPalUserAgreementUrl)
    }

    @Test
    fun `payPalDirectBaseUrl forwards the versioned url from configuration`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        assertEquals("https://www.paypal.com/v1/", sut.payPalDirectBaseUrl)
    }

    @Test
    fun `payPalEnvironment forwards the value from configuration`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        assertEquals("live", sut.payPalEnvironment)
    }

    @Test
    fun `isPayPalTouchDisabled forwards the value from configuration`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        assertTrue(sut.isPayPalTouchDisabled)
    }

    @Test
    fun `payPalCurrencyIsoCode forwards the value from configuration`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_LIVE_PAYPAL)
        assertEquals("USD", sut.payPalCurrencyIsoCode)
    }

    @Test
    fun `when visa checkout api key exists, isVisaCheckoutEnabled is true`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_VISA_CHECKOUT)
        assertTrue(sut.isVisaCheckoutEnabled)
    }

    @Test
    fun `visaCheckoutSupportedNetworks forwards the value from configuration`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_VISA_CHECKOUT)
        val expected = listOf("AMEX", "DISCOVER", "MASTERCARD", "VISA")
        assertEquals(expected, sut.visaCheckoutSupportedNetworks)
    }

    @Test
    fun `visaCheckoutApiKey forwards the value from configuration`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_VISA_CHECKOUT)
        assertEquals("gwApikey", sut.visaCheckoutApiKey)
    }

    @Test
    fun `visaCheckoutExternalClientId forwards the value from configuration`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_VISA_CHECKOUT)
        assertEquals("gwExternalClientId", sut.visaCheckoutExternalClientId)
    }

    @Test
    fun `when google pay is enabled in configuration, isGooglePayEnabled is true`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY)
        assertTrue(sut.isGooglePayEnabled)
    }

    @Test
    fun `googlePayAuthorizationFingerprint forwards the value from configuration`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY)
        assertEquals("google-auth-fingerprint", sut.googlePayAuthorizationFingerprint)
    }

    @Test
    fun `googlePayEnvironment forwards the value from configuration`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY)
        assertEquals("sandbox", sut.googlePayEnvironment)
    }

    @Test
    fun `googlePayDisplayName forwards the value from configuration`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY)
        assertEquals("Google Pay Merchant", sut.googlePayDisplayName)
    }

    @Test
    fun `googlePaySupportedNetworks forwards the values from configuration`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY)
        val expected = listOf("visa", "mastercard", "amex", "discover")
        assertEquals(expected, sut.googlePaySupportedNetworks)
    }

    @Test
    fun `googlePayPayPalClientId forwards the value from configuration`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GOOGLE_PAY)
        assertEquals("pay-pal-client-id", sut.googlePayPayPalClientId)
    }

    @Test
    fun `when access token is present, isBraintreeApiEnabled returns true`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN)
        assertTrue(sut.isBraintreeApiEnabled)
    }

    @Test
    fun `when access token is not present, isBraintreeApiEnabled returns false`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITHOUT_ACCESS_TOKEN)
        assertFalse(sut.isBraintreeApiEnabled)
    }

    @Test
    fun `braintreeApiAccessToken forwards the value from configuration`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN)
        assertEquals("access-token-example", sut.braintreeApiAccessToken)
    }

    @Test
    fun `braintreeApiUrl forwards the value from configuration`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_ACCESS_TOKEN)
        assertEquals("https://braintree-api.com", sut.braintreeApiUrl)
    }

    @Test
    fun `when the feature is enabled, isGraphQLFeatureEnabled returns true`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL)
        assertTrue(sut.isGraphQLFeatureEnabled(GraphQLConstants.Features.TOKENIZE_CREDIT_CARDS))
    }

    @Test
    fun `when the feature is not enabled, isGraphQLFeatureEnabled returns false`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL)
        assertFalse(sut.isGraphQLFeatureEnabled("a_different_feature"))
    }

    @Test
    fun `graphQLUrl forwards the value from configuration`() {
        val sut = Configuration.fromJson(Fixtures.CONFIGURATION_WITH_GRAPHQL)
        assertEquals("https://example-graphql.com/graphql", sut.graphQLUrl)
    }

    @Test
    fun `when loadConfiguration succeeds, fetch calls back with configuration`() = runTest {
        val expectedConfiguration: Configuration = mockk()
        coEvery {
            configurationLoader.loadConfiguration(authorization)
        } returns ConfigurationLoaderResult.Success(expectedConfiguration)

        var capturedConfiguration: Configuration? = null
        var capturedError: Exception? = null

        val testDispatcher = StandardTestDispatcher(testScheduler)
        Configuration.fetch(
            configurationLoader,
            authorization,
            TestScope(testDispatcher),
            ConfigurationCallback { configuration, error ->
                capturedConfiguration = configuration
                capturedError = error
            }
        )

        advanceUntilIdle()

        assertEquals(expectedConfiguration, capturedConfiguration)
        assertNull(capturedError)
    }

    @Test
    fun `when loadConfiguration fails, fetch calls back with error`() = runTest {
        val expectedError = ConfigurationException("configuration fetch failed")
        coEvery {
            configurationLoader.loadConfiguration(authorization)
        } returns ConfigurationLoaderResult.Failure(expectedError)

        var capturedConfiguration: Configuration? = null
        var capturedError: Exception? = null

        val testDispatcher = StandardTestDispatcher(testScheduler)
        Configuration.fetch(
            configurationLoader,
            authorization,
            TestScope(testDispatcher),
            ConfigurationCallback { configuration, error ->
                capturedConfiguration = configuration
                capturedError = error
            }
        )

        advanceUntilIdle()

        assertNull(capturedConfiguration)
        assertEquals(expectedError, capturedError)
    }

    @Test
    fun `when loadConfiguration is cancelled, fetch does not call back`() = runTest {
        coEvery {
            configurationLoader.loadConfiguration(authorization)
        } throws CancellationException()

        var callbackInvoked = false
        val testDispatcher = StandardTestDispatcher(testScheduler)
        Configuration.fetch(
            configurationLoader,
            authorization,
            TestScope(testDispatcher),
            ConfigurationCallback { _, _ -> callbackInvoked = true }
        )

        advanceUntilIdle()

        assertFalse(callbackInvoked)
    }
}
