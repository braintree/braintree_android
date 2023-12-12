package com.braintreepayments.api

import io.mockk.mockk
import kotlin.test.assertIs
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for BraintreeShopperInsightsClient.
 *
 * This class contains tests for the shopper insights functionality within the Braintree SDK.
 * It focuses on testing how the client handles different scenarios when fetching recommended
 * payment methods.
 */
class ShopperInsightsClientUnitTest {

    private lateinit var sut: ShopperInsightsClient
    private lateinit var paymentApi: PaymentReadyApi

    @Before
    fun beforeEach() {
        paymentApi = mockk(relaxed = true)
        sut = ShopperInsightsClient(paymentApi)
    }

    /**
     * Tests if the getRecommendedPaymentMethods method returns paypal and venmo recommendations
     * when providing a shopping insight request.
     */
    @Test
    fun testGetRecommendedPaymentMethods_returnsDefaultRecommendations() {
        val request = ShopperInsightsRequest.Email("fake-email")
        sut.getRecommendedPaymentMethods(request
        ) { result ->
            assertNotNull(result)
            val successResult = assertIs<ShopperInsightsResult.Success>(result)
            assertNotNull(successResult.response.isPayPalRecommended)
            assertNotNull(successResult.response.isVenmoRecommended)
        }
    }
}
