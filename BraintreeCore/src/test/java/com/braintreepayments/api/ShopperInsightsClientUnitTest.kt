package com.braintreepayments.api

import io.mockk.mockk
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class ShopperInsightsClientUnitTest {

    private lateinit var httpClient: BraintreeHttpClient
    private lateinit var sut: BraintreeShopperInsightsClient

    @Before
    fun beforeEach() {
        httpClient = mockk(relaxed = true)
        sut = BraintreeShopperInsightsClient(httpClient = httpClient)
    }

    @Test
    fun testGetRecommendedPaymentMethods_returnsDefaultRecommendations() {
        val request = ShopperInsightRequest(
            email = "fake-email",
            phoneCountryCode = "fake-country-code",
            phoneNationalNumber = "fake-national-phone"
        )
        val result = sut.getRecommendedPaymentMethods(request)
        assertNotNull(result?.isPayPalRecommended)
        assertNotNull(result?.isVenmoRecommended)
    }
}