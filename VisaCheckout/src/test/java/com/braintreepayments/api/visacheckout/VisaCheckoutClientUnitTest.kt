package com.braintreepayments.api.visacheckout

import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.Configuration.Companion.fromJson
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkApiClientBuilder
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import com.braintreepayments.api.testutils.TestConfigurationBuilder
import com.braintreepayments.api.testutils.TestConfigurationBuilder.TestVisaCheckoutConfigurationBuilder
import com.braintreepayments.api.core.ConfigurationException
import com.visa.checkout.Profile.CardBrand
import com.visa.checkout.VisaPaymentSummary
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch

@RunWith(RobolectricTestRunner::class)
class VisaCheckoutClientUnitTest {

    private lateinit var configurationWithVisaCheckout: Configuration
    private lateinit var visaPaymentSummary: VisaPaymentSummary

    @Before
    @Throws(Exception::class)
    fun setup() {
        configurationWithVisaCheckout = fromJson(Fixtures.CONFIGURATION_WITH_VISA_CHECKOUT)
        visaPaymentSummary = mockk(relaxed = true)

        every { visaPaymentSummary.callId } returns "stubbedCallId"
        every { visaPaymentSummary.encKey } returns "stubbedEncKey"
        every { visaPaymentSummary.encPaymentData } returns "stubbedEncPaymentData"
    }

    @Test
    fun createProfileBuilder_whenNotEnabled_throwsConfigurationException() {
        val apiClient = MockkApiClientBuilder().build()
        val configuration = TestConfigurationBuilder.basicConfig<Configuration>()
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()
        val sut = VisaCheckoutClient(
            braintreeClient,
            apiClient
        )
        val listener = mockk<VisaCheckoutCreateProfileBuilderCallback>(relaxed = true)
        sut.createProfileBuilder(listener)

        val configurationExceptionSlot = slot<VisaCheckoutProfileBuilderResult>()
        verify(exactly = 1) {
            listener.onVisaCheckoutProfileBuilderResult(
                capture(
                    configurationExceptionSlot
                )
            )
        }

        val profileBuilderResult = configurationExceptionSlot.captured
        assertTrue(profileBuilderResult is VisaCheckoutProfileBuilderResult.Failure)
        val exception = (profileBuilderResult as VisaCheckoutProfileBuilderResult.Failure).error
        assertEquals("Visa Checkout is not enabled.", exception.message)
    }

    @Test
    @Throws(Exception::class)
    fun createProfileBuilder_whenProduction_usesProductionConfig() {
        val lock = CountDownLatch(1)
        val apiClient = MockkApiClientBuilder().build()
        val configString = TestConfigurationBuilder()
            .environment("production")
            .visaCheckout(
                TestVisaCheckoutConfigurationBuilder()
                    .apikey("gwApiKey")
                    .supportedCardTypes(CardBrand.VISA, CardBrand.MASTERCARD)
                    .externalClientId("gwExternalClientId")
            )
            .build()
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(fromJson(configString))
            .build()
        val sut = VisaCheckoutClient(
            braintreeClient,
            apiClient
        )
        sut.createProfileBuilder { profileBuilderResult ->
            assertTrue(profileBuilderResult is VisaCheckoutProfileBuilderResult.Success)
            val profileBuilder = (profileBuilderResult as VisaCheckoutProfileBuilderResult
            .Success).profileBuilder
            val profile = profileBuilder.build()
            assertNotNull(profile)
            assertTrue(profile.acceptedCardBrands.contains(CardBrand.VISA))
            assertTrue(profile.acceptedCardBrands.contains(CardBrand.MASTERCARD))
            lock.countDown()
        }
        lock.await()
    }

    @Test
    @Throws(Exception::class)
    fun createProfileBuilder_whenNotProduction_usesSandboxConfig() {
        val lock = CountDownLatch(1)
        val apiClient = MockkApiClientBuilder().build()
        val configString = TestConfigurationBuilder()
            .environment("environment")
            .visaCheckout(
                TestVisaCheckoutConfigurationBuilder()
                    .apikey("gwApiKey")
                    .supportedCardTypes(CardBrand.VISA, CardBrand.MASTERCARD)
                    .externalClientId("gwExternalClientId")
            )
            .build()
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(fromJson(configString))
            .build()
        val sut = VisaCheckoutClient(
            braintreeClient,
            apiClient
        )
        sut.createProfileBuilder { profileBuilderResult ->
            val profileBuilder = (profileBuilderResult as VisaCheckoutProfileBuilderResult
            .Success).profileBuilder
            val profile = profileBuilder.build()
            assertNotNull(profile)
            assertTrue(profile.acceptedCardBrands.contains(CardBrand.VISA))
            assertTrue(profile.acceptedCardBrands.contains(CardBrand.MASTERCARD))
            lock.countDown()
        }
        lock.await()
    }

    @Test
    fun `when createProfileBuilder is called and configuration is null, exception is returned as a failure`() {
        val exception = Exception()
        val callback = mockk<VisaCheckoutCreateProfileBuilderCallback>(relaxed = true)
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationError(exception)
            .build()
        val sut = VisaCheckoutClient(braintreeClient, mockk())

        sut.createProfileBuilder(callback)

        verify {
            callback.onVisaCheckoutProfileBuilderResult(withArg { failure ->
                assertTrue(failure is VisaCheckoutProfileBuilderResult.Failure)
                assertEquals(exception, (failure as VisaCheckoutProfileBuilderResult.Failure).error)
            })
        }
    }

    @Test
    fun `when createProfileBuilder is called and configuration and exception are null, a failure is returned`() {
        val exception = ConfigurationException("Error getting configuration.")
        val callback = mockk<VisaCheckoutCreateProfileBuilderCallback>(relaxed = true)
        val braintreeClient = MockkBraintreeClientBuilder().build()
        val sut = VisaCheckoutClient(braintreeClient, mockk())

        sut.createProfileBuilder(callback)

        verify {
            callback.onVisaCheckoutProfileBuilderResult(withArg { failure ->
                assertTrue(failure is VisaCheckoutProfileBuilderResult.Failure)
                val error = (failure as VisaCheckoutProfileBuilderResult.Failure).error
                assertTrue(error is ConfigurationException)
                assertEquals("Error getting configuration.", error.message)
            })
        }
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_whenSuccessful_postsVisaPaymentMethodNonce() {
        val apiClient = MockkApiClientBuilder()
            .tokenizeRESTSuccess(JSONObject(Fixtures.PAYMENT_METHODS_VISA_CHECKOUT_RESPONSE))
            .build()
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configurationWithVisaCheckout)
            .build()
        val sut = VisaCheckoutClient(
            braintreeClient,
            apiClient
        )
        sut.tokenize(visaPaymentSummary) { visaCheckoutResult ->
            assertTrue(visaCheckoutResult is VisaCheckoutResult.Success)
            val nonce = (visaCheckoutResult as VisaCheckoutResult.Success).nonce
            assertNotNull(nonce)
        }
    }

    @Test
    @Throws(JSONException::class)
    fun tokenize_whenSuccessful_sendsAnalyticEvent() {
        val apiClient = MockkApiClientBuilder()
            .tokenizeRESTSuccess(JSONObject(Fixtures.PAYMENT_METHODS_VISA_CHECKOUT_RESPONSE))
            .build()
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configurationWithVisaCheckout)
            .build()
        val sut = VisaCheckoutClient(
            braintreeClient,
            apiClient
        )
        val listener = mockk<VisaCheckoutTokenizeCallback>(relaxed = true)
        sut.tokenize(visaPaymentSummary, listener)
        verify { braintreeClient.sendAnalyticsEvent(VisaCheckoutAnalytics.TOKENIZE_STARTED) }
        verify { braintreeClient.sendAnalyticsEvent(VisaCheckoutAnalytics.TOKENIZE_SUCCEEDED) }
    }

    @Test
    fun tokenize_whenFailure_postsException() {
        val tokenizeError = Exception("Mock Failure")
        val apiClient = MockkApiClientBuilder()
            .tokenizeRESTError(tokenizeError)
            .build()
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configurationWithVisaCheckout)
            .build()
        val sut = VisaCheckoutClient(
            braintreeClient,
            apiClient
        )
        sut.tokenize(visaPaymentSummary) { visaCheckoutResult ->
            assertTrue(visaCheckoutResult is VisaCheckoutResult.Failure)
            val error = (visaCheckoutResult as VisaCheckoutResult.Failure).error
            assertEquals(tokenizeError, error)
        }
    }

    @Test
    fun tokenize_whenFailure_sendsAnalyticEvent() {
        val tokenizeError = Exception("Mock Failure")
        val apiClient = MockkApiClientBuilder()
            .tokenizeRESTError(tokenizeError)
            .build()
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configurationWithVisaCheckout)
            .build()
        val sut = VisaCheckoutClient(
            braintreeClient,
            apiClient
        )
        val listener = mockk<VisaCheckoutTokenizeCallback>(relaxed = true)
        sut.tokenize(visaPaymentSummary, listener)
        verify { braintreeClient.sendAnalyticsEvent(VisaCheckoutAnalytics.TOKENIZE_STARTED) }
        verify { braintreeClient.sendAnalyticsEvent(VisaCheckoutAnalytics.TOKENIZE_FAILED) }
    }
}