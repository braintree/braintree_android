package com.braintreepayments.api

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import kotlin.test.assertIs

/**
 * Unit tests for BraintreeShopperInsightsClient.
 *
 * This class contains tests for the shopper insights functionality within the Braintree SDK.
 * It focuses on testing how the client handles different scenarios when fetching recommended
 * payment methods.
 */
class ShopperInsightsClientUnitTest {

    private lateinit var sut: BraintreeShopperInsightsClient

    @Before
    fun beforeEach() {
        sut = BraintreeShopperInsightsClient()
    }

    /**
     * Tests if the getRecommendedPaymentMethods method returns paypal and venmo recommendations
     * when providing a shopping insight request.
     */
    @Test
    fun testGetRecommendedPaymentMethods_returnsDefaultRecommendations() = runBlocking {
        val request = ShopperInsightRequest(
            email = "fake-email",
            phoneCountryCode = "fake-country-code",
            phoneNationalNumber = "fake-national-phone"
        )
        val resultDeferred = CompletableDeferred<ShopperInsightResult>()
        sut.getRecommendedPaymentMethods(request, object : ShopperInsightCallback{
            override fun onResult(result: ShopperInsightResult) {
                resultDeferred.complete(result)
            }
        })
        val result = resultDeferred.await()
        assertNotNull(result)
        val successResult = assertIs<ShopperInsightResult.Success>(result)
        assertNotNull(successResult.response.isPayPalRecommended)
        assertNotNull(successResult.response.isVenmoRecommended)
    }
}