package com.braintreepayments.api.shopperinsights.v2

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.braintreepayments.api.core.Authorization
import com.braintreepayments.api.core.Configuration
import com.braintreepayments.api.core.ExperimentalBetaApi
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.SharedPreferencesHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4ClassRunner::class)
@OptIn(ExperimentalBetaApi::class)
class ShopperInsightsClientV2Test {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun createCustomerSession_withInvalidAuth_returnsFailure() {
        val sut = ShopperInsightsClientV2(context, Fixtures.TOKENIZATION_KEY)
        val latch = CountDownLatch(1)
        var result: CustomerSessionResult? = null

        sut.createCustomerSession(CustomerSessionRequest()) { sessionResult ->
            result = sessionResult
            latch.countDown()
        }

        latch.await(20, TimeUnit.SECONDS)
        assertTrue(
            "Expected Failure but got ${result?.javaClass?.simpleName}",
            result is CustomerSessionResult.Failure
        )
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun updateCustomerSession_withInvalidAuth_returnsFailure() {
        val sut = ShopperInsightsClientV2(context, Fixtures.TOKENIZATION_KEY)
        val latch = CountDownLatch(1)
        var result: CustomerSessionResult? = null

        sut.updateCustomerSession(
            CustomerSessionRequest(),
            "fake-session-id"
        ) { sessionResult ->
            result = sessionResult
            latch.countDown()
        }

        latch.await(20, TimeUnit.SECONDS)
        assertTrue(
            "Expected Failure but got ${result?.javaClass?.simpleName}",
            result is CustomerSessionResult.Failure
        )
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun generateCustomerRecommendations_withInvalidAuth_returnsFailure() {
        val sut = ShopperInsightsClientV2(context, Fixtures.TOKENIZATION_KEY)
        val latch = CountDownLatch(1)
        var result: CustomerRecommendationsResult? = null

        sut.generateCustomerRecommendations(
            CustomerSessionRequest(),
            "fake-session-id"
        ) { recommendationsResult ->
            result = recommendationsResult
            latch.countDown()
        }

        latch.await(20, TimeUnit.SECONDS)
        assertTrue(
            "Expected Failure but got ${result?.javaClass?.simpleName}",
            result is CustomerRecommendationsResult.Failure
        )
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun generateCustomerRecommendations_withCachedConfig_returnsFailureWithError() {
        cacheConfiguration()

        val sut = ShopperInsightsClientV2(context, Fixtures.TOKENIZATION_KEY)
        val latch = CountDownLatch(1)
        var result: CustomerRecommendationsResult? = null

        sut.generateCustomerRecommendations(
            CustomerSessionRequest(
                hashedEmail = "abc123",
                hashedPhoneNumber = "def456"
            ),
            "test-session-id"
        ) { recommendationsResult ->
            result = recommendationsResult
            latch.countDown()
        }

        latch.await(20, TimeUnit.SECONDS)
        assertTrue(result is CustomerRecommendationsResult.Failure)
        assertNotNull((result as CustomerRecommendationsResult.Failure).error)
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun generateCustomerRecommendations_withNullRequest_returnsFailure() {
        cacheConfiguration()

        val sut = ShopperInsightsClientV2(context, Fixtures.TOKENIZATION_KEY)
        val latch = CountDownLatch(1)
        var result: CustomerRecommendationsResult? = null

        sut.generateCustomerRecommendations(
            customerSessionRequest = null,
            sessionId = "test-session-id"
        ) { recommendationsResult ->
            result = recommendationsResult
            latch.countDown()
        }

        latch.await(20, TimeUnit.SECONDS)
        assertTrue(
            "Expected Failure but got ${result?.javaClass?.simpleName}",
            result is CustomerRecommendationsResult.Failure
        )
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun generateCustomerRecommendations_withNullSessionId_returnsFailure() {
        cacheConfiguration()

        val sut = ShopperInsightsClientV2(context, Fixtures.TOKENIZATION_KEY)
        val latch = CountDownLatch(1)
        var result: CustomerRecommendationsResult? = null

        sut.generateCustomerRecommendations(
            customerSessionRequest = CustomerSessionRequest(hashedEmail = "abc123"),
            sessionId = null
        ) { recommendationsResult ->
            result = recommendationsResult
            latch.countDown()
        }

        latch.await(20, TimeUnit.SECONDS)
        assertTrue(
            "Expected Failure but got ${result?.javaClass?.simpleName}",
            result is CustomerRecommendationsResult.Failure
        )
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun generateCustomerRecommendations_withPurchaseUnits_returnsFailure() {
        cacheConfiguration()

        val sut = ShopperInsightsClientV2(context, Fixtures.TOKENIZATION_KEY)
        val latch = CountDownLatch(1)
        var result: CustomerRecommendationsResult? = null

        sut.generateCustomerRecommendations(
            CustomerSessionRequest(
                hashedEmail = "abc123",
                purchaseUnits = listOf(
                    PurchaseUnit(amount = "100.00", currencyCode = "USD"),
                    PurchaseUnit(amount = "200.00", currencyCode = "EUR")
                )
            ),
            "test-session-id"
        ) { recommendationsResult ->
            result = recommendationsResult
            latch.countDown()
        }

        latch.await(20, TimeUnit.SECONDS)
        assertTrue(
            "Expected Failure but got ${result?.javaClass?.simpleName}",
            result is CustomerRecommendationsResult.Failure
        )
    }

    @Test(timeout = 1000)
    fun customerSessionRequest_constructsWithAllFields() {
        val request = CustomerSessionRequest(
            hashedEmail = "abc123",
            hashedPhoneNumber = "def456",
            payPalAppInstalled = true,
            venmoAppInstalled = false,
            purchaseUnits = listOf(PurchaseUnit("10.00", "USD"))
        )

        assertEquals("abc123", request.hashedEmail)
        assertEquals("def456", request.hashedPhoneNumber)
        assertEquals(true, request.payPalAppInstalled)
        assertEquals(false, request.venmoAppInstalled)
        assertEquals(1, request.purchaseUnits?.size)
        assertEquals("10.00", request.purchaseUnits?.first()?.amount)
        assertEquals("USD", request.purchaseUnits?.first()?.currencyCode)
    }

    @Test(timeout = 1000)
    fun customerSessionRequest_withDefaults_hasNullFields() {
        val request = CustomerSessionRequest()

        assertNull(request.hashedEmail)
        assertNull(request.hashedPhoneNumber)
        assertNull(request.payPalAppInstalled)
        assertNull(request.venmoAppInstalled)
        assertNull(request.purchaseUnits)
    }

    @Test(timeout = 1000)
    fun customerSessionRequest_fieldMutation_updatesValues() {
        val request = CustomerSessionRequest()

        request.hashedEmail = "updated-hash"
        request.hashedPhoneNumber = "updated-phone-hash"
        request.payPalAppInstalled = true
        request.venmoAppInstalled = true
        request.purchaseUnits = listOf(PurchaseUnit("50.00", "GBP"))

        assertEquals("updated-hash", request.hashedEmail)
        assertEquals("updated-phone-hash", request.hashedPhoneNumber)
        assertEquals(true, request.payPalAppInstalled)
        assertEquals(true, request.venmoAppInstalled)
        assertEquals("50.00", request.purchaseUnits?.first()?.amount)
    }

    @Test(timeout = 1000)
    fun customerSessionRequest_withMultiplePurchaseUnits_constructsCorrectly() {
        val request = CustomerSessionRequest(
            purchaseUnits = listOf(
                PurchaseUnit("100.00", "USD"),
                PurchaseUnit("200.00", "EUR"),
                PurchaseUnit("300.00", "GBP")
            )
        )

        assertEquals(3, request.purchaseUnits?.size)
        assertEquals("100.00", request.purchaseUnits?.get(0)?.amount)
        assertEquals("EUR", request.purchaseUnits?.get(1)?.currencyCode)
        assertEquals("300.00", request.purchaseUnits?.get(2)?.amount)
    }

    @Test(timeout = 1000)
    fun purchaseUnit_constructsWithExpectedFields() {
        val purchaseUnit = PurchaseUnit(amount = "99.99", currencyCode = "EUR")

        assertEquals("99.99", purchaseUnit.amount)
        assertEquals("EUR", purchaseUnit.currencyCode)
    }

    @Test(timeout = 1000)
    fun paymentOptions_constructsWithExpectedFields() {
        val options = PaymentOptions(paymentOption = "PAYPAL", recommendedPriority = 1)

        assertEquals("PAYPAL", options.paymentOption)
        assertEquals(1, options.recommendedPriority)
    }

    private fun cacheConfiguration() {
        val authorization = Authorization.fromString(Fixtures.TOKENIZATION_KEY)
        val configuration = Configuration.fromJson(Fixtures.SANDBOX_CONFIGURATION_WITH_GRAPHQL)
        SharedPreferencesHelper.overrideConfigurationCache(context, authorization, configuration)
    }
}
