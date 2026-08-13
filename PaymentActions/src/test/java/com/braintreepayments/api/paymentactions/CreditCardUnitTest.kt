package com.braintreepayments.api.paymentactions

import org.json.JSONObject
import org.junit.Test
import org.skyscreamer.jsonassert.JSONAssert

class CreditCardUnitTest {

    @Test
    fun `toGraphQLVariables returns full nested shape when all fields are present`() {
        val creditCard = CreditCard(
            number = "4111111111111111",
            expirationMonth = "12",
            expirationYear = "2028",
            cvv = "123",
            cardholderName = "Jane Doe",
            streetAddress = "123 Main St",
            extendedAddress = "Apt 4",
            locality = "Chicago",
            region = "IL",
            postalCode = "60601",
            countryCodeAlpha2 = "US",
        )

        val expected = JSONObject(
            """
            {
                "paymentMethodDetails": {
                    "creditCard": {
                        "number": "4111111111111111",
                        "expirationMonth": "12",
                        "expirationYear": "2028",
                        "cvv": "123",
                        "cardholderName": "Jane Doe",
                        "billingAddress": {
                            "streetAddress": "123 Main St",
                            "extendedAddress": "Apt 4",
                            "locality": "Chicago",
                            "region": "IL",
                            "postalCode": "60601",
                            "countryCodeAlpha2": "US"
                        }
                    }
                }
            }
            """.trimIndent()
        )

        JSONAssert.assertEquals(expected, creditCard.toGraphQLVariables(), true)
    }

    @Test
    fun `toGraphQLVariables omits optional fields when not provided`() {
        val creditCard = CreditCard(
            number = "4111111111111111",
            expirationMonth = "12",
            expirationYear = "2028",
        )

        val expected = JSONObject(
            """
            {
                "paymentMethodDetails": {
                    "creditCard": {
                        "number": "4111111111111111",
                        "expirationMonth": "12",
                        "expirationYear": "2028"
                    }
                }
            }
            """.trimIndent()
        )

        JSONAssert.assertEquals(expected, creditCard.toGraphQLVariables(), true)
    }

    @Test
    fun `toGraphQLVariables omits billingAddress fields that are not provided`() {
        val creditCard = CreditCard(
            number = "4111111111111111",
            expirationMonth = "12",
            expirationYear = "2028",
            postalCode = "60601",
            countryCodeAlpha2 = "US",
        )

        val expected = JSONObject(
            """
            {
                "paymentMethodDetails": {
                    "creditCard": {
                        "number": "4111111111111111",
                        "expirationMonth": "12",
                        "expirationYear": "2028",
                        "billingAddress": {
                            "postalCode": "60601",
                            "countryCodeAlpha2": "US"
                        }
                    }
                }
            }
            """.trimIndent()
        )

        JSONAssert.assertEquals(expected, creditCard.toGraphQLVariables(), true)
    }

    @Test
    fun `paymentActionSelectionSet returns the default id and status selection`() {
        val creditCard = CreditCard(
            number = "4111111111111111",
            expirationMonth = "12",
            expirationYear = "2028",
        )

        val expected = "id\nstatus"

        kotlin.test.assertEquals(expected, creditCard.paymentActionSelectionSet())
    }
}
