package com.braintreepayments.api.shopperinsights

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
@Suppress("DEPRECATION")
class ShopperInsightsClientTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun getRecommendedPaymentMethods_withNullEmailAndPhone_returnsFailureWithMessage() {
        val sut = ShopperInsightsClient(context, Fixtures.TOKENIZATION_KEY)
        val latch = CountDownLatch(1)
        var result: ShopperInsightsResult? = null

        sut.getRecommendedPaymentMethods(
            ShopperInsightsRequest(email = null, phone = null)
        ) { shopperResult ->
            result = shopperResult
            latch.countDown()
        }

        latch.await(20, TimeUnit.SECONDS)
        assertTrue(result is ShopperInsightsResult.Failure)
        val errorMessage = requireNotNull((result as ShopperInsightsResult.Failure).error.message)
        assertTrue(errorMessage.contains("email"))
        assertTrue(errorMessage.contains("phone"))
        assertTrue(errorMessage.contains("non-null"))
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun getRecommendedPaymentMethods_withTokenizationKey_returnsClientTokenError() {
        val sut = ShopperInsightsClient(context, Fixtures.TOKENIZATION_KEY)
        val latch = CountDownLatch(1)
        var result: ShopperInsightsResult? = null

        sut.getRecommendedPaymentMethods(
            ShopperInsightsRequest(email = "test@example.com", phone = null)
        ) { shopperResult ->
            result = shopperResult
            latch.countDown()
        }

        latch.await(20, TimeUnit.SECONDS)
        assertTrue(result is ShopperInsightsResult.Failure)
        val errorMessage = requireNotNull((result as ShopperInsightsResult.Failure).error.message)
        assertTrue(errorMessage.contains("client token"))
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun getRecommendedPaymentMethods_withPhoneOnly_andTokenizationKey_returnsFailure() {
        val sut = ShopperInsightsClient(context, Fixtures.TOKENIZATION_KEY)
        val latch = CountDownLatch(1)
        var result: ShopperInsightsResult? = null

        sut.getRecommendedPaymentMethods(
            ShopperInsightsRequest(
                email = null,
                phone = ShopperInsightsBuyerPhone(
                    countryCode = "1",
                    nationalNumber = "5551234567"
                )
            )
        ) { shopperResult ->
            result = shopperResult
            latch.countDown()
        }

        latch.await(20, TimeUnit.SECONDS)
        assertTrue(
            "Expected Failure but got ${result?.javaClass?.simpleName}",
            result is ShopperInsightsResult.Failure
        )
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun getRecommendedPaymentMethods_withInvalidAuthorization_returnsFailure() {
        val sut = ShopperInsightsClient(context, "sandbox_invalid_key_xxxxxxxx")
        val latch = CountDownLatch(1)
        var result: ShopperInsightsResult? = null

        sut.getRecommendedPaymentMethods(
            ShopperInsightsRequest(email = "test@example.com", phone = null)
        ) { shopperResult ->
            result = shopperResult
            latch.countDown()
        }

        latch.await(20, TimeUnit.SECONDS)
        assertTrue(
            "Expected Failure but got ${result?.javaClass?.simpleName}",
            result is ShopperInsightsResult.Failure
        )
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun getRecommendedPaymentMethods_withClientTokenAndEmail_returnsResult() {
        cacheConfiguration()

        val sut = ShopperInsightsClient(context, Fixtures.BASE64_CLIENT_TOKEN)
        val latch = CountDownLatch(1)
        var result: ShopperInsightsResult? = null

        sut.getRecommendedPaymentMethods(
            ShopperInsightsRequest(email = "test@example.com", phone = null)
        ) { shopperResult ->
            result = shopperResult
            latch.countDown()
        }

        latch.await(20, TimeUnit.SECONDS)
        assertNotNull("Expected a non-null result", result)
        assertTrue(
            "Expected Success or Failure",
            result is ShopperInsightsResult.Success || result is ShopperInsightsResult.Failure
        )
    }

    @Test(timeout = 30000)
    @Throws(InterruptedException::class)
    fun getRecommendedPaymentMethods_withClientTokenAndPhone_returnsResult() {
        cacheConfiguration()

        val sut = ShopperInsightsClient(context, Fixtures.BASE64_CLIENT_TOKEN)
        val latch = CountDownLatch(1)
        var result: ShopperInsightsResult? = null

        sut.getRecommendedPaymentMethods(
            ShopperInsightsRequest(
                email = null,
                phone = ShopperInsightsBuyerPhone(
                    countryCode = "1",
                    nationalNumber = "5551234567"
                )
            )
        ) { shopperResult ->
            result = shopperResult
            latch.countDown()
        }

        latch.await(20, TimeUnit.SECONDS)
        assertNotNull("Expected a non-null result", result)
    }

    @Test(timeout = 1000)
    fun shopperInsightsRequest_constructsWithAllFields() {
        val phone = ShopperInsightsBuyerPhone(
            countryCode = "1",
            nationalNumber = "5551234567"
        )
        val request = ShopperInsightsRequest(
            email = "test@example.com",
            phone = phone
        )

        assertEquals("test@example.com", request.email)
        assertEquals("1", request.phone?.countryCode)
        assertEquals("5551234567", request.phone?.nationalNumber)
    }

    @Test(timeout = 1000)
    fun shopperInsightsRequest_withEmailOnly_hasNullPhone() {
        val request = ShopperInsightsRequest(email = "test@example.com", phone = null)

        assertEquals("test@example.com", request.email)
        assertNull(request.phone)
    }

    @Test(timeout = 1000)
    fun shopperInsightsRequest_withPhoneOnly_hasNullEmail() {
        val request = ShopperInsightsRequest(
            email = null,
            phone = ShopperInsightsBuyerPhone(countryCode = "1", nationalNumber = "5551234567")
        )

        assertNull(request.email)
        assertEquals("1", request.phone?.countryCode)
    }

    @Test(timeout = 1000)
    fun shopperInsightsRequest_fieldMutation_updatesValues() {
        val request = ShopperInsightsRequest(email = "original@example.com", phone = null)

        request.email = "updated@example.com"
        request.phone = ShopperInsightsBuyerPhone(countryCode = "44", nationalNumber = "7911123456")

        assertEquals("updated@example.com", request.email)
        assertEquals("44", request.phone?.countryCode)
        assertEquals("7911123456", request.phone?.nationalNumber)
    }

    @Test(timeout = 1000)
    fun shopperInsightsBuyerPhone_constructsWithExpectedFields() {
        val phone = ShopperInsightsBuyerPhone(countryCode = "44", nationalNumber = "7911123456")

        assertEquals("44", phone.countryCode)
        assertEquals("7911123456", phone.nationalNumber)
    }

    @Test(timeout = 1000)
    fun shopperInsightsBuyerPhone_fieldMutation_updatesValues() {
        val phone = ShopperInsightsBuyerPhone(countryCode = "1", nationalNumber = "5551234567")

        phone.countryCode = "44"
        phone.nationalNumber = "7911123456"

        assertEquals("44", phone.countryCode)
        assertEquals("7911123456", phone.nationalNumber)
    }

    @Test(timeout = 1000)
    fun buttonType_hasExpectedValues() {
        val values = ButtonType.entries
        assertEquals(4, values.size)
        assertTrue(values.contains(ButtonType.PAYPAL))
        assertTrue(values.contains(ButtonType.VENMO))
        assertTrue(values.contains(ButtonType.PAYPAL_PAY_LATER))
        assertTrue(values.contains(ButtonType.OTHER))
    }

    @Test(timeout = 1000)
    fun buttonOrder_hasExpectedValues() {
        val values = ButtonOrder.entries
        assertEquals(9, values.size)
        assertTrue(values.contains(ButtonOrder.FIRST))
        assertTrue(values.contains(ButtonOrder.Eighth))
        assertTrue(values.contains(ButtonOrder.OTHER))
    }

    @Test(timeout = 1000)
    fun pageType_hasExpectedValues() {
        val values = PageType.entries
        assertEquals(13, values.size)
        assertTrue(values.contains(PageType.HOMEPAGE))
        assertTrue(values.contains(PageType.CHECKOUT))
        assertTrue(values.contains(PageType.OTHER))
    }

    @Test(timeout = 1000)
    fun experimentType_hasExpectedValues() {
        val values = ExperimentType.entries
        assertEquals(2, values.size)
        assertTrue(values.contains(ExperimentType.CONTROL))
        assertTrue(values.contains(ExperimentType.TEST))
    }

    @Test(timeout = 1000)
    fun presentmentDetails_constructsWithExpectedFields() {
        val details = PresentmentDetails(
            type = ExperimentType.TEST,
            buttonOrder = ButtonOrder.FIRST,
            pageType = PageType.CHECKOUT
        )

        assertEquals(ExperimentType.TEST, details.type)
        assertEquals(ButtonOrder.FIRST, details.buttonOrder)
        assertEquals(PageType.CHECKOUT, details.pageType)
    }

    private fun cacheConfiguration() {
        val authorization = Authorization.fromString(Fixtures.BASE64_CLIENT_TOKEN)
        val configuration = Configuration.fromJson(Fixtures.SANDBOX_CONFIGURATION_WITH_GRAPHQL)
        SharedPreferencesHelper.overrideConfigurationCache(context, authorization, configuration)
    }
}
