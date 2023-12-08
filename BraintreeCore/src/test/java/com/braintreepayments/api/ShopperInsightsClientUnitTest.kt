package com.braintreepayments.api

import io.mockk.CapturingSlot
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import kotlin.test.assertIs
import org.json.JSONObject

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
        val request = ShopperInsightsRequest.Email("fake-email")
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
        val testCountryCode = "1"
        val testNationalNumber = "123456789"
        val request = ShopperInsightsRequest.Phone(
            BuyerPhone(
                countryCode = testCountryCode,
                nationalNumber = testNationalNumber
            )
        )
        val slot = CapturingSlot<String>()
        sut.getRecommendedPaymentMethods(request) {
            verify {
                paymentApi.processRequest(capture(slot))
            }
        }

        val customer = JSONObject(slot.captured).getJSONObject("customer")
        val phone = customer.getJSONObject("phone")
        val countryCode = phone.getString("countryCode")
        val nationalNumber = phone.getString("nationalNumber")

        assertEquals(testCountryCode, countryCode)
        assertEquals(testNationalNumber, nationalNumber)
    }

    /**
     * Tests if the getRecommendedPaymentMethods method passes correct email body JSON to payment api
     * when providing a email request object.
     */
    @Test
    fun testGetRecommendedPaymentMethods_verifyEmailJson() {
        val fakeEmail = "fake-email"
        val request = ShopperInsightsRequest.Email(fakeEmail)
        val slot = CapturingSlot<String>()
        sut.getRecommendedPaymentMethods(request) {
            verify {
                paymentApi.processRequest(capture(slot))
            }
        }

        val customer = JSONObject(slot.captured).getJSONObject("customer")
        val email = customer.getString("email")

        assertEquals(fakeEmail, email)
    }

    /**
     * Tests if the getRecommendedPaymentMethods method passes correct email and phone body JSON to payment api
     * when providing a email and phone request object.
     */
    @Test
    fun testGetRecommendedPaymentMethods_verifyEmailAndPhoneJson() {
        val fakeEmail = "fake-email"
        val testCountryCode = "1"
        val testNationalNumber = "123456789"

        val request = ShopperInsightsRequest.EmailAndPhone(
            email = fakeEmail,
            phone = BuyerPhone(
                countryCode = testCountryCode,
                nationalNumber = testNationalNumber
            )
        )

        val slot = CapturingSlot<String>()
        sut.getRecommendedPaymentMethods(request) {
            verify {
                paymentApi.processRequest(capture(slot))
            }
        }

        val customer = JSONObject(slot.captured).getJSONObject("customer")
        val email = customer.getString("email")
        val phone = customer.getJSONObject("phone")
        val countryCode = phone.getString("countryCode")
        val nationalNumber = phone.getString("nationalNumber")

        assertEquals(fakeEmail, email)
        assertEquals(testCountryCode, countryCode)
        assertEquals(testNationalNumber, nationalNumber)
    }
}
