package com.braintreepayments.api

import com.braintreepayments.api.EligiblePaymentsApiRequest.Companion.toJson
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

class EligiblePaymentsApiRequestUnitTest {

    @Test
    fun `test json string conversion`() {
        val email = "fake-email@email-provider.com"
        val testCountryCode = "1"
        val testNationalNumber = "123456789"
        val request = EligiblePaymentsApiRequest(
            request = ShopperInsightsRequest(
                email,
                ShopperInsightsBuyerPhone(
                    countryCode = testCountryCode,
                    nationalNumber = testNationalNumber
                )
            ),
            currencyCode = "USD",
            merchantId = "MXSJ4F5BADVNS",
            countryCode = "US",
            accountDetails = true,
            constraintType = "INCLUDE",
            paymentSources = listOf("PAYPAL", "VENMO")
        )

        val observedJsonString = request.toJson()
        val expectedJsonString = """
            {
                "customer": {
                    "country_code": "US",
                    "email": "fake-email@email-provider.com",
                    "phone": {
                        "country_code": "1",
                        "national_number": "123456789"
                    }
                },
                "purchase_units": [
                    {
                        "amount": {
                            "currency_code": "USD"
                        }
                    }
                ],
                "preferences": {
                    "include_account_details": true,
                    "payment_source_constraint": {
                        "constraint_type": "INCLUDE",
                        "payment_sources": [
                            "PAYPAL",
                            "VENMO"
                        ]  
                    }
                }
            }
        """.trimIndent()

        JSONAssert.assertEquals(expectedJsonString, observedJsonString, true)
    }
}
