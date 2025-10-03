package com.braintreepayments.api.americanexpress

import com.braintreepayments.api.core.AnalyticsEventParams
import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.sharedutils.AuthorizationException
import com.braintreepayments.api.sharedutils.HttpResponseCallback
import com.braintreepayments.api.testutils.Fixtures
import com.braintreepayments.api.testutils.MockkBraintreeClientBuilder
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AmericanExpressClientUnitTest {

    private lateinit var amexRewardsCallback: AmericanExpressGetRewardsBalanceCallback

    @Before
    fun beforeEach() {
        amexRewardsCallback = mockk(relaxed = true)
    }

    @Test
    fun getRewardsBalance_sendsGETRequestForAmexAwardsBalance() {
        val braintreeClient = mockk<BraintreeClient>(relaxed = true)
        val sut = AmericanExpressClient(braintreeClient)
        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback)

        val urlSlot = slot<String>()
        verify { braintreeClient.sendGET(capture(urlSlot), any(HttpResponseCallback::class)) }

        val url = urlSlot.captured
        assertEquals(
            "/v1/payment_methods/amex_rewards_balance?paymentMethodNonce=fake-nonce&currencyIsoCode=USD",
            url
        )
    }

    @Test
    fun getRewardsBalance_callsListenerWithRewardsBalanceOnSuccess() {
        val braintreeClient = MockkBraintreeClientBuilder().sendGetSuccessfulResponse(
            Fixtures.AMEX_REWARDS_BALANCE_SUCCESS).build()

        val sut = AmericanExpressClient(braintreeClient)
        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback)
        val amexRewardsSlot = slot<AmericanExpressResult>()
        verify { amexRewardsCallback.onAmericanExpressResult(capture(amexRewardsSlot)) }

        val result = amexRewardsSlot.captured
        assertTrue(result is AmericanExpressResult.Success)

        val rewardsBalance = (result as AmericanExpressResult.Success).rewardsBalance
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
    fun getRewardsBalance_callsListenerWithRewardsBalanceWithErrorCode_OnIneligibleCard() {
        val braintreeClient = MockkBraintreeClientBuilder().sendGetSuccessfulResponse(
            Fixtures.AMEX_REWARDS_BALANCE_INELIGIBLE_CARD).build()

        val sut = AmericanExpressClient(braintreeClient)
        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback)

        val amexRewardsSlot = slot<AmericanExpressResult>()
        verify { amexRewardsCallback.onAmericanExpressResult(capture(amexRewardsSlot)) }

        val result = amexRewardsSlot.captured
        assertTrue(result is AmericanExpressResult.Success)

        val rewardsBalance = (result as AmericanExpressResult.Success).rewardsBalance
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
    fun getRewardsBalance_callsListenerWithRewardsBalanceWithErrorCode_OnInsufficientPoints() {
        val braintreeClient = MockkBraintreeClientBuilder().sendGetSuccessfulResponse(
            Fixtures.AMEX_REWARDS_BALANCE_INSUFFICIENT_POINTS).build()

        val sut = AmericanExpressClient(braintreeClient)
        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback)

        val amexRewardsSlot = slot<AmericanExpressResult>()
        verify { amexRewardsCallback.onAmericanExpressResult(capture(amexRewardsSlot)) }

        val result = amexRewardsSlot.captured
        assertTrue(result is AmericanExpressResult.Success)

        val rewardsBalance = (result as AmericanExpressResult.Success).rewardsBalance
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
    fun getRewardsBalance_callsBackFailure_OnHttpError() {
        val expectedError = Exception("error")
        val braintreeClient = MockkBraintreeClientBuilder().sendGetErrorResponse(
            expectedError).build()

        val sut = AmericanExpressClient(braintreeClient)
        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback)

        val amexRewardsSlot = slot<AmericanExpressResult>()
        verify { amexRewardsCallback.onAmericanExpressResult(capture(amexRewardsSlot)) }

        val result = amexRewardsSlot.captured
        assertTrue(result is AmericanExpressResult.Failure)

        val actualError = (result as AmericanExpressResult.Failure).error
        assertEquals(expectedError, actualError)
    }

    @Test
    fun getRewardsBalance_sendsAnalyticsEventOnSuccess() {
        val braintreeClient = MockkBraintreeClientBuilder().sendGetSuccessfulResponse(
            Fixtures.AMEX_REWARDS_BALANCE_SUCCESS).build()

        val sut = AmericanExpressClient(braintreeClient)
        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback)

        val params = AnalyticsEventParams()
        verify { braintreeClient.sendAnalyticsEvent(AmericanExpressAnalytics.REWARDS_BALANCE_STARTED, params, true) }
        verify {
            braintreeClient.sendAnalyticsEvent(AmericanExpressAnalytics.REWARDS_BALANCE_SUCCEEDED, params, true)
        }
    }

    @Test
    fun getRewardsBalance_sendsAnalyticsEventOnFailure() {
        val braintreeClient = MockkBraintreeClientBuilder().sendGetErrorResponse(
            AuthorizationException("Bad fingerprint")).build()

        val sut = AmericanExpressClient(braintreeClient)
        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback)

        val params = AnalyticsEventParams()
        val errorParams = AnalyticsEventParams(errorDescription = "Bad fingerprint")

        verify { braintreeClient.sendAnalyticsEvent(AmericanExpressAnalytics.REWARDS_BALANCE_STARTED, params, true) }
        verify {
            braintreeClient.sendAnalyticsEvent(AmericanExpressAnalytics.REWARDS_BALANCE_FAILED, errorParams, true)
        }
    }

    @Test
    fun getRewardsBalance_sendsAnalyticsEventOnParseError() {
        val notJson = "Big blob that is not a valid JSON object"
        val braintreeClient = MockkBraintreeClientBuilder().sendGetSuccessfulResponse(notJson).build()

        val sut = AmericanExpressClient(braintreeClient)
        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback)

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
