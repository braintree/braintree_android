package com.braintreepayments.api.americanexpress

import com.braintreepayments.api.core.BraintreeClient
import com.braintreepayments.api.sharedutils.HttpResponseCallback
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals

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
}