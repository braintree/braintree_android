package com.braintreepayments.api.visacheckout

import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.Configuration.Companion.fromJson
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkApiClientBuilder
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import com.braintreepayments.api.testutils.TestConfigurationBuilder
import com.braintreepayments.api.testutils.TestConfigurationBuilder.TestVisaCheckoutConfigurationBuilder
import com.visa.checkout.Profile.CardBrand
import com.visa.checkout.VisaPaymentSummary
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class VisaCheckoutClientUnitTest {

    private lateinit var configurationWithVisaCheckout: Configuration
    private lateinit var visaPaymentSummary: VisaPaymentSummary

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testScope: TestScope

    @Before
    @Throws(Exception::class)
    fun setup() {
        configurationWithVisaCheckout = fromJson(Fixtures.CONFIGURATION_WITH_VISA_CHECKOUT)
        visaPaymentSummary = mockk(relaxed = true)
        testScope = TestScope(testDispatcher)

        every { visaPaymentSummary.callId } returns "stubbedCallId"
        every { visaPaymentSummary.encKey } returns "stubbedEncKey"
        every { visaPaymentSummary.encPaymentData } returns "stubbedEncPaymentData"
    }

    @Test
    fun `when createProfileBuilder is called and visa checkout is not enabled, callback is invoked with configuration exception failure`() = runTest(testDispatcher) {
        val apiClient = MockkApiClientBuilder().build()
        val configuration = TestConfigurationBuilder.basicConfig<Configuration>()
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configuration)
            .build()
        val sut = VisaCheckoutClient(
            braintreeClient,
            apiClient,
            testDispatcher,
            this
        )
        val listener = mockk<VisaCheckoutCreateProfileBuilderCallback>(relaxed = true)
        sut.createProfileBuilder(listener)
        advanceUntilIdle()

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
    fun `when createProfileBuilder is called with a production environment configuration, callback is invoked with success containing profile builder with accepted card brands`() = runTest(testDispatcher) {
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
            apiClient,
            testDispatcher,
            this
        )
        sut.createProfileBuilder { profileBuilderResult ->
            assertTrue(profileBuilderResult is VisaCheckoutProfileBuilderResult.Success)
            val profileBuilder = (profileBuilderResult as VisaCheckoutProfileBuilderResult
            .Success).profileBuilder
            val profile = profileBuilder.build()
            assertNotNull(profile)
            assertTrue(profile.acceptedCardBrands.contains(CardBrand.VISA))
            assertTrue(profile.acceptedCardBrands.contains(CardBrand.MASTERCARD))
        }
        advanceUntilIdle()
    }

    @Test
    @Throws(Exception::class)
    fun `when createProfileBuilder is called with a non-production environment configuration, callback is invoked with success containing profile builder with accepted card brands`() = runTest(testDispatcher) {
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
            apiClient,
            testDispatcher,
            this
        )
        sut.createProfileBuilder { profileBuilderResult ->
            val profileBuilder = (profileBuilderResult as VisaCheckoutProfileBuilderResult
            .Success).profileBuilder
            val profile = profileBuilder.build()
            assertNotNull(profile)
            assertTrue(profile.acceptedCardBrands.contains(CardBrand.VISA))
            assertTrue(profile.acceptedCardBrands.contains(CardBrand.MASTERCARD))
        }
        advanceUntilIdle()
    }

    @Test
    fun `when createProfileBuilder is called and configuration fetch fails, exception is returned as a failure`() = runTest(testDispatcher) {
        val exception = IOException("test error")
        val callback = mockk<VisaCheckoutCreateProfileBuilderCallback>(relaxed = true)
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationError(exception)
            .build()
        val sut = VisaCheckoutClient(braintreeClient, mockk(), testDispatcher, this)

        sut.createProfileBuilder(callback)
        advanceUntilIdle()

        verify {
            callback.onVisaCheckoutProfileBuilderResult(withArg { failure ->
                assertTrue(failure is VisaCheckoutProfileBuilderResult.Failure)
                assertEquals(exception, (failure as VisaCheckoutProfileBuilderResult.Failure).error)
            })
        }
    }

    @Test
    fun `when createProfileBuilder is called and braintreeClient throws cancellation exception, callback is not invoked`() = runTest(testDispatcher) {
        val apiClient = MockkApiClientBuilder().build()
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationError(kotlin.coroutines.cancellation.CancellationException("cancelled"))
            .build()
        val sut = VisaCheckoutClient(braintreeClient, apiClient, testDispatcher, testScope)
        val listener = mockk<VisaCheckoutCreateProfileBuilderCallback>(relaxed = true)
        sut.createProfileBuilder(listener)
        advanceUntilIdle()

        verify(exactly = 0) { listener.onVisaCheckoutProfileBuilderResult(any()) }
    }

    @Test
    @Throws(JSONException::class)
    fun `when tokenize is successful, callback is invoked with success containing visa payment method nonce`() {
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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    @Throws(JSONException::class)
    fun `when tokenize is successful, started and succeeded analytics events are sent`() = runTest(testDispatcher) {
        val apiClient = MockkApiClientBuilder()
            .tokenizeRESTSuccess(JSONObject(Fixtures.PAYMENT_METHODS_VISA_CHECKOUT_RESPONSE))
            .build()
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configurationWithVisaCheckout)
            .build()
        val sut = VisaCheckoutClient(
            braintreeClient,
            apiClient,
            testDispatcher,
            testScope
        )
        val listener = mockk<VisaCheckoutTokenizeCallback>(relaxed = true)
        sut.tokenize(visaPaymentSummary, listener)
        advanceUntilIdle()
        verify { braintreeClient.sendAnalyticsEvent(VisaCheckoutAnalytics.TOKENIZE_STARTED) }
        verify { braintreeClient.sendAnalyticsEvent(VisaCheckoutAnalytics.TOKENIZE_SUCCEEDED) }
    }

    @Test
    fun `when tokenize fails, callback is invoked with failure containing the tokenize error`() {
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

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when tokenize fails, started and failed analytics events are sent`() = runTest(testDispatcher) {
        val tokenizeError = Exception("Mock Failure")
        val apiClient = MockkApiClientBuilder()
            .tokenizeRESTError(tokenizeError)
            .build()
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configurationWithVisaCheckout)
            .build()
        val sut = VisaCheckoutClient(
            braintreeClient,
            apiClient,
            testDispatcher,
            testScope
        )
        val listener = mockk<VisaCheckoutTokenizeCallback>(relaxed = true)
        sut.tokenize(visaPaymentSummary, listener)
        advanceUntilIdle()
        verify { braintreeClient.sendAnalyticsEvent(VisaCheckoutAnalytics.TOKENIZE_STARTED) }
        verify {
            braintreeClient.sendAnalyticsEvent(
                VisaCheckoutAnalytics.TOKENIZE_FAILED,
                AnalyticsEventParams(errorDescription = tokenizeError.message)
            )
        }
    }

    @Test
    fun `when tokenize is called and apiClient throws cancellation exception, callback is not invoked`() = runTest(testDispatcher) {
        val apiClient = MockkApiClientBuilder()
            .tokenizeRESTError(kotlin.coroutines.cancellation.CancellationException("cancelled"))
            .build()
        val braintreeClient = MockkBraintreeClientBuilder()
            .configurationSuccess(configurationWithVisaCheckout)
            .build()
        val sut = VisaCheckoutClient(braintreeClient, apiClient, testDispatcher, testScope)
        val listener = mockk<VisaCheckoutTokenizeCallback>(relaxed = true)
        sut.tokenize(visaPaymentSummary, listener)
        advanceUntilIdle()

        verify(exactly = 0) { listener.onVisaCheckoutResult(any()) }
    }
}