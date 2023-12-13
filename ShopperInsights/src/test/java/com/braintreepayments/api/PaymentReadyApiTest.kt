package com.braintreepayments.api

import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

class PaymentReadyApiTest {

    private val paymentReadyApi = PaymentReadyApi()

    @Test
    fun `test phone to json string conversion`() {
        val testCountryCode = "1"
        val testNationalNumber = "123456789"
        val request = ShopperInsightsRequest(
            null,
            BuyerPhone(
                countryCode = testCountryCode,
                nationalNumber = testNationalNumber
            )
        )

        val observedJsonString = paymentReadyApi.processRequest(request)
        val expectedJsonString = """
            {
                "customer": {
                    "phone": {
                        "countryCode": "$testCountryCode",
                        "nationalNumber": "$testNationalNumber"
                    }
                }
            }
        """.trimIndent()

        JSONAssert.assertEquals(expectedJsonString, observedJsonString, true)
    }

    @Test
    fun `test email to json string conversion`() {
        val email = "fake-email@email-provider.com"
        val request = ShopperInsightsRequest(email, null)

        val observedJsonString = paymentReadyApi.processRequest(request)
        val expectedJsonString = """
            {
                "customer": {
                    "email": "$email"
                }
            }
        """.trimIndent()

        JSONAssert.assertEquals(expectedJsonString, observedJsonString, true)
    }

    @Test
    fun `test email and phone to json string conversion`() {
        val email = "fake-email@email-provider.com"
        val testCountryCode = "1"
        val testNationalNumber = "123456789"
        val request = ShopperInsightsRequest(
            email,
            BuyerPhone(
                countryCode = testCountryCode,
                nationalNumber = testNationalNumber
            )
        )

        val observedJsonString = paymentReadyApi.processRequest(request)
        val expectedJsonString = """
            {
                "customer": {
                    "phone": {
                        "countryCode": "$testCountryCode",
                        "nationalNumber": "$testNationalNumber"
                    },
                    "email": "$email"
                }
            }
        """.trimIndent()

        JSONAssert.assertEquals(expectedJsonString, observedJsonString, true)
    }
}
