package com.braintreepayments.api

import io.mockk.mockk
import io.mockk.verify
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

    private lateinit var sut: ShopperInsightsClient
    private lateinit var paymentApi: PaymentReadyAPI

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
        val request = ShopperInsightRequest.Email(
            BuyerEmail("fake-email")
        )
        sut.getRecommendedPaymentMethods(request
        ) { result ->
            assertNotNull(result)
            val successResult = assertIs<ShopperInsightResult.Success>(result)
            assertNotNull(successResult.response.isPayPalRecommended)
            assertNotNull(successResult.response.isVenmoRecommended)
        }
    }

    /**
     * Tests if the getRecommendedPaymentMethods method passes correct phone body JSON to payment api
     * when providing a phone request object.
     */
    @Test
    fun testGetRecommendedPaymentMethods_verifyPhoneJson() {
        val request = ShopperInsightRequest.Phone(
            BuyerPhone(
                countryCode = "1",
                nationalNumber = "123456789"
            )
        )
        sut.getRecommendedPaymentMethods(request) {
            verify {
                paymentApi.processRequest(
                    "{\"customer\": {\"phone\": {\"countryCode\": \"1\", \"nationalNumber\": \"123456789\"}}}"
                )
            }
        }
    }

    /**
     * Tests if the getRecommendedPaymentMethods method passes correct email body JSON to payment api
     * when providing a email request object.
     */
    @Test
    fun testGetRecommendedPaymentMethods_verifyEmailJson() {
        val request = ShopperInsightRequest.Email(
            BuyerEmail("fake-email"),
        )
        sut.getRecommendedPaymentMethods(request) {
            verify {
                paymentApi.processRequest(
                    "{\"customer\": {\"email\": \"fake-email\"}}"
                )
            }
        }
    }


    /**
     * Tests if the getRecommendedPaymentMethods method passes correct email and phone body JSON to payment api
     * when providing a email and phone request object.
     */
    @Test
    fun testGetRecommendedPaymentMethods_verifyEmailAndPhoneJson() {
        val request = ShopperInsightRequest.EmailAndPhone(
            BuyerEmail("fake-email"),
            BuyerPhone(
                countryCode = "1",
                nationalNumber = "123456789"
            )
        )
        sut.getRecommendedPaymentMethods(request) {
            verify {
                paymentApi.processRequest(
                    "{\"customer\": {\"email\": \"fake-email\",\"phone\": {\"countryCode\": \"1\", \"nationalNumber\": \"123456789\"}}}"
                )
            }
        }
    }
}
