package com.braintreepayments.api

import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

class ShoppingInsightsCreateBodyTest {

    private val shoppingInsightsCreateBody = ShoppingInsightsCreateBody()

    @Test
    fun `test email and phone to json string conversion`() {
        val email = "fake-email@email-provider.com"
        val testCountryCode = "1"
        val testNationalNumber = "123456789"
        val request = ShopperInsightsApiRequest(
            request = ShopperInsightsRequest(
                email,
                BuyerPhone(
                    countryCode = testCountryCode,
                    nationalNumber = testNationalNumber
                )
            ),
            currencyCode = "USD",
            merchantId = "MXSJ4F5BADVNS",
            countryCode = "US",
            accountDetails = true,
            vaultTokens = true,
            constraintType = "INCLUDE",
            paymentSources = listOf("PAYPAL", "VENMO")
        )

        val observedJsonString = shoppingInsightsCreateBody.execute(request)
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
                        "payee": {
                            "merchant_id": "MXSJ4F5BADVNS"
                        },
                        "amount": {
                            "currency_code": "USD"
                        }
                    }
            
                ],
                "preferences": {
                    "include_account_details": true,
                    "include_vault_tokens": true,
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
