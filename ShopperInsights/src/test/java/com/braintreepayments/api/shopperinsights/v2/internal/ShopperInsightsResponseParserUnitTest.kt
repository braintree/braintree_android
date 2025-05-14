package com.braintreepayments.api.shopperinsights.v2.internal

import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ShopperInsightsResponseParserUnitTest {

    private val responseParser = ShopperInsightsResponseParser()

    @Test
    fun `parseSessionId returns sessionId when response is valid`() {
        val responseBody = """
            {
                "data": {
                    "createCustomerSession": {
                        "sessionId": "test-session-id"
                    }
                }
            }
        """.trimIndent()

        val sessionId = responseParser.parseSessionId(responseBody, "createCustomerSession")
        assertEquals("test-session-id", sessionId)
    }

    @Test
    fun `parseSessionId throws JSONException when response is missing sessionId`() {
        val responseBody = """
            {
                "data": {
                    "createCustomerSession": {}
                }
            }
        """.trimIndent()

        assertThrows(JSONException::class.java) {
            responseParser.parseSessionId(responseBody, "createCustomerSession")
        }
    }

    @Test
    fun `parseSessionId throws JSONException when response is missing data`() {
        val responseBody = """
            {
                "invalidKey": {}
            }
        """.trimIndent()

        assertThrows(JSONException::class.java) {
            responseParser.parseSessionId(responseBody, "createCustomerSession")
        }
    }

    @Test
    fun `parseSessionId throws JSONException when response is malformed`() {
        val responseBody = "invalid-json"

        assertThrows(JSONException::class.java) {
            responseParser.parseSessionId(responseBody, "createCustomerSession")
        }
    }
}
