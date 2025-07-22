package com.braintreepayments.api.americanexpress

import com.braintreepayments.api.core.BraintreeClient
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
        val braintreeClient:BraintreeClient = mockk(relaxed = true)
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
        val braintreeClient:BraintreeClient = MockkBraintreeClientBuilder().sendGETSuccessfulResponse(
            Fixtures.AMEX_REWARDS_BALANCE_SUCCESS).build()

        val sut = AmericanExpressClient(braintreeClient)
        sut.getRewardsBalance("fake-nonce", "USD", amexRewardsCallback)
        val amexRewardsSlot = slot<AmericanExpressResult>()
        verify {  amexRewardsCallback.onAmericanExpressResult(capture(amexRewardsSlot)) }

        val result = amexRewardsSlot.captured
        assertTrue { result is AmericanExpressResult.Success }

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
}