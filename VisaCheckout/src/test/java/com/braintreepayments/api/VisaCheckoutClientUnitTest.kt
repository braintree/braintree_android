package com.braintreepayments.api

import com.braintreepayments.api.Configuration.Companion.fromJson
import com.braintreepayments.api.TestConfigurationBuilder.TestVisaCheckoutConfigurationBuilder
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Arrays
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
        val sut = VisaCheckoutClient(braintreeClient, apiClient)
        val listener = mockk<VisaCheckoutCreateProfileBuilderCallback>(relaxed = true)
        sut.createProfileBuilder(listener)

        val configurationExceptionSlot = slot<ConfigurationException>()
        verify(exactly = 1) { listener.onResult(null, capture(configurationExceptionSlot)) }

        val configurationException = configurationExceptionSlot.captured
        assertEquals("Visa Checkout is not enabled.", configurationException.message)
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
        val sut = VisaCheckoutClient(braintreeClient, apiClient)
        sut.createProfileBuilder { profileBuilder, error ->
            val expectedCardBrands = Arrays.asList(CardBrand.VISA, CardBrand.MASTERCARD)
            val profile = profileBuilder!!.build()
            assertNotNull(profile)
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
        val sut = VisaCheckoutClient(braintreeClient, apiClient)
        sut.createProfileBuilder { profileBuilder, error ->
            val expectedCardBrands = Arrays.asList(CardBrand.VISA, CardBrand.MASTERCARD)
            val profile = profileBuilder!!.build()
            assertNotNull(profile)
            lock.countDown()
        }
        lock.await()
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
        val sut = VisaCheckoutClient(braintreeClient, apiClient)
        val listener = mockk<VisaCheckoutTokenizeCallback>(relaxed = true)
        sut.tokenize(visaPaymentSummary, listener)
        verify { listener.onResult(any(), null) }
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
        val sut = VisaCheckoutClient(braintreeClient, apiClient)
        val listener = mockk<VisaCheckoutTokenizeCallback>(relaxed = true)
        sut.tokenize(visaPaymentSummary, listener)
        verify { braintreeClient.sendAnalyticsEvent("visacheckout.tokenize.succeeded", null, null, false, -1, -1, null) }
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
        val sut = VisaCheckoutClient(braintreeClient, apiClient)
        val listener = mockk<VisaCheckoutTokenizeCallback>(relaxed = true)
        sut.tokenize(visaPaymentSummary, listener)
        verify { listener.onResult(null, tokenizeError) }
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
        val sut = VisaCheckoutClient(braintreeClient, apiClient)
        val listener = mockk<VisaCheckoutTokenizeCallback>(relaxed = true)
        sut.tokenize(visaPaymentSummary, listener)
        verify { braintreeClient.sendAnalyticsEvent("visacheckout.tokenize.failed", null, null,false, -1, -1, null) }
    }
}