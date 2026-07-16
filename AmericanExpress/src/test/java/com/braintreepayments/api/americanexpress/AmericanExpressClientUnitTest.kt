package com.braintreepayments.api.americanexpress

import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.sharedutils.AuthorizationException
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AmericanExpressClientUnitTest {

    private lateinit var amexRewardsCallback: AmericanExpressGetRewardsBalanceCallback

    @Before
    fun beforeEach() {
        amexRewardsCallback = mockk(relaxed = true)
    }

    @Test
    fun `when getRewardsBalance is called, GET request includes nonce and currency code in query params`() = runTest {
        val braintreeClient = mockk<BraintreeClient>(relaxed = true)
        coEvery { braintreeClient.sendGET(any()) } returns """
        {
            "conversionRate": "0.0070",
            "currencyAmount": "316795.03",
            "currencyIsoCode": "USD",
            "requestId": "request-id",
            "rewardsAmount": "45256433",
            "rewardsUnit": "Points"
        }
        """.trimIndent()
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = AmericanExpressClient(braintreeClient, testDispatcher, testScope)

        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback)
        advanceUntilIdle()

        val urlSlot = slot<String>()
        coVerify { braintreeClient.sendGET(capture(urlSlot)) }

        val url = urlSlot.captured
        assertEquals(
            "/v1/payment_methods/amex_rewards_balance?paymentMethodNonce=fake-nonce&currencyIsoCode=USD",
            url
        )
    }

    @Test
    fun `when getRewardsBalance receives a successful response, callback receives success result with rewards balance`() = runTest {
        val braintreeClient = MockkBraintreeClientBuilder().sendGetSuccessfulResponse(
            Fixtures.AMEX_REWARDS_BALANCE_SUCCESS).build()

        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = AmericanExpressClient(braintreeClient, testDispatcher, testScope)
        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback)
        advanceUntilIdle()
        val amexRewardsSlot = slot<AmericanExpressResult>()
        verify { amexRewardsCallback.onAmericanExpressResult(capture(amexRewardsSlot)) }

        val result = amexRewardsSlot.captured
        assertTrue(result is AmericanExpressResult.Success)

        val rewardsBalance = result.rewardsBalance
        assertNotNull(rewardsBalance)
        assertEquals("0.0070", rewardsBalance.conversionRate)
        assertEquals("316795.03", rewardsBalance.currencyAmount)
        assertEquals("USD", rewardsBalance.currencyIsoCode)
        assertEquals("715f4712-8690-49ed-8cc5-d7fb1c2d", rewardsBalance.requestId)
        assertEquals("45256433", rewardsBalance.rewardsAmount)
        assertEquals("Points", rewardsBalance.rewardsUnit)
        assertNull(rewardsBalance.errorCode)
        assertNull(rewardsBalance.errorMessage)
    }

    @Test
    fun `when getRewardsBalance is called and card is ineligible, callback receives success result with error code`() = runTest {
        val braintreeClient = MockkBraintreeClientBuilder().sendGetSuccessfulResponse(
            Fixtures.AMEX_REWARDS_BALANCE_INELIGIBLE_CARD).build()
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = AmericanExpressClient(braintreeClient, testDispatcher, testScope)
        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback)
        advanceUntilIdle()
        val amexRewardsSlot = slot<AmericanExpressResult>()
        verify { amexRewardsCallback.onAmericanExpressResult(capture(amexRewardsSlot)) }

        val result = amexRewardsSlot.captured
        assertTrue(result is AmericanExpressResult.Success)

        val rewardsBalance = result.rewardsBalance
        assertNotNull(rewardsBalance)
        assertNull(rewardsBalance.conversionRate)
        assertNull(rewardsBalance.currencyAmount)
        assertNull(rewardsBalance.currencyIsoCode)
        assertNull(rewardsBalance.requestId)
        assertNull(rewardsBalance.rewardsAmount)
        assertNull(rewardsBalance.rewardsUnit)
        assertEquals("INQ2002", rewardsBalance.errorCode)
        assertEquals("Card is ineligible", rewardsBalance.errorMessage)
    }

    @Test
    fun `when getRewardsBalance is called and points are insufficient, callback receives success result with error code`() = runTest {
        val braintreeClient = MockkBraintreeClientBuilder().sendGetSuccessfulResponse(
            Fixtures.AMEX_REWARDS_BALANCE_INSUFFICIENT_POINTS).build()
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = AmericanExpressClient(braintreeClient, testDispatcher, testScope)
        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback)
        advanceUntilIdle()
        val amexRewardsSlot = slot<AmericanExpressResult>()
        verify { amexRewardsCallback.onAmericanExpressResult(capture(amexRewardsSlot)) }

        val result = amexRewardsSlot.captured
        assertTrue(result is AmericanExpressResult.Success)

        val rewardsBalance = result.rewardsBalance
        assertNotNull(rewardsBalance)
        assertNull(rewardsBalance.conversionRate)
        assertNull(rewardsBalance.currencyAmount)
        assertNull(rewardsBalance.currencyIsoCode)
        assertNull(rewardsBalance.requestId)
        assertNull(rewardsBalance.rewardsAmount)
        assertNull(rewardsBalance.rewardsUnit)
        assertEquals("INQ2003", rewardsBalance.errorCode)
        assertEquals("Insufficient points on card", rewardsBalance.errorMessage)
    }

    @Test
    fun `when getRewardsBalance http request errors, callback receives failure result`() = runTest {
        val expectedError = IOException("error")
        val braintreeClient = MockkBraintreeClientBuilder().sendGetErrorResponse(
            expectedError).build()
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = AmericanExpressClient(braintreeClient, testDispatcher, testScope)
        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback)
        advanceUntilIdle()
        val amexRewardsSlot = slot<AmericanExpressResult>()
        verify { amexRewardsCallback.onAmericanExpressResult(capture(amexRewardsSlot)) }

        val result = amexRewardsSlot.captured
        assertTrue(result is AmericanExpressResult.Failure)

        val actualError = result.error
        assertEquals(expectedError, actualError)
    }

    @Test
    fun `when getRewardsBalance succeeds, started and succeeded analytics events are sent`() = runTest {
        val braintreeClient = MockkBraintreeClientBuilder().sendGetSuccessfulResponse(
            Fixtures.AMEX_REWARDS_BALANCE_SUCCESS).build()
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = AmericanExpressClient(braintreeClient, testDispatcher, testScope)
        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback)
        advanceUntilIdle()
        val params = AnalyticsEventParams()
        verify { braintreeClient.sendAnalyticsEvent(AmericanExpressAnalytics.REWARDS_BALANCE_STARTED, params, true) }
        verify {
            braintreeClient.sendAnalyticsEvent(AmericanExpressAnalytics.REWARDS_BALANCE_SUCCEEDED, params, true)
        }
    }

    @Test
    fun `when getRewardsBalance http request fails, started and failed analytics events are sent with error description`() = runTest {
        val braintreeClient = MockkBraintreeClientBuilder().sendGetErrorResponse(
            AuthorizationException("Bad fingerprint")).build()
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = AmericanExpressClient(braintreeClient, testDispatcher, testScope)
        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback)
        advanceUntilIdle()
        val params = AnalyticsEventParams()
        val errorParams = AnalyticsEventParams(errorDescription = "Bad fingerprint")

        verify { braintreeClient.sendAnalyticsEvent(AmericanExpressAnalytics.REWARDS_BALANCE_STARTED, params, true) }
        verify {
            braintreeClient.sendAnalyticsEvent(AmericanExpressAnalytics.REWARDS_BALANCE_FAILED, errorParams, true)
        }
    }

    @Test
    fun `when getRewardsBalance response has a json parse error, started and failed analytics events are sent with error description`() = runTest {
        val notJson = "Big blob that is not a valid JSON object"
        val braintreeClient = MockkBraintreeClientBuilder().sendGetSuccessfulResponse(notJson).build()
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val testScope = TestScope(testDispatcher)
        val sut = AmericanExpressClient(braintreeClient, testDispatcher, testScope)
        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback)
        advanceUntilIdle()
        val params = AnalyticsEventParams()
        val errorParams = AnalyticsEventParams(
            errorDescription =
                "Value ${notJson.split(" ")[0]} of type java.lang.String cannot be converted to JSONObject"
        )

        verify { braintreeClient.sendAnalyticsEvent(AmericanExpressAnalytics.REWARDS_BALANCE_STARTED, params, true) }
        verify {
            braintreeClient.sendAnalyticsEvent(AmericanExpressAnalytics.REWARDS_BALANCE_FAILED, errorParams, true)
        }
    }
}
