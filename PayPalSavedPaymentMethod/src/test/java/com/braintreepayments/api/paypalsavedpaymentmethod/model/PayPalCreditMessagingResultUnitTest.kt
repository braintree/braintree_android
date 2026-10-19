package com.braintreepayments.api.paypalsavedpaymentmethod.model

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PayPalCreditMessagingResultUnitTest {

    @Test
    fun `fromJson parses all items and metadata`() {
        val json = JSONObject(
            """
            {
              "messages": [
                {
                  "preferred_message": {
                    "id": "message-1",
                    "type": "PLST_SQ",
                    "content": {
                      "main_items": [ { "type": "TEXT", "text": "Pay in 4" } ],
                      "disclaimer_items": [ { "type": "TEXT", "text": "See terms" } ],
                      "action_items": [ { "type": "LINK", "text": "Learn more", "click_url": "https://paypal.com" } ]
                    },
                    "analytics": { "impression_url": "https://paypal.com/impression" }
                  }
                }
              ]
            }
            """.trimIndent()
        )

        val result = requireNotNull(PayPalCreditMessagingResult.fromJson(json))

        assertEquals("Pay in 4", result.mainItems.single().text)
        assertEquals("See terms", result.disclaimerItems.single().text)
        assertEquals("Learn more", result.actionItems.single().text)
        assertEquals("message-1", result.messageId)
        assertEquals("PLST_SQ", result.messageType)
        assertEquals("https://paypal.com/impression", result.impressionUrl)
    }

    @Test
    fun `fromJson returns null when mainItems has no displayable text`() {
        assertNull(PayPalCreditMessagingResult.fromJson(messageWithMainItems("""[ { "type": "TEXT", "text": "" } ]""")))
    }

    @Test
    fun `fromJson treats an image block's alternativeText as displayable text`() {
        val json = messageWithMainItems("""[ { "type": "IMAGE", "alternative_text": "PayPal" } ]""")

        assertNotNull(PayPalCreditMessagingResult.fromJson(json))
    }

    @Test
    fun `fromJson drops malformed entries from content item arrays`() {
        val json = messageWithMainItems("""[ "not-an-object", { "type": "TEXT", "text": "Pay in 4" } ]""")

        assertEquals(1, requireNotNull(PayPalCreditMessagingResult.fromJson(json)).mainItems.size)
    }

    @Test
    fun `fromJson returns null when preferred_message has no content`() {
        val json = JSONObject("""{ "messages": [ { "preferred_message": { "id": "message-1" } } ] }""")

        assertNull(PayPalCreditMessagingResult.fromJson(json))
    }

    @Test
    fun `fromJson returns null when there is no preferred_message`() {
        assertNull(PayPalCreditMessagingResult.fromJson(JSONObject("""{ "messages": [] }""")))
    }

    @Test
    fun `fromJson returns a null impressionUrl when analytics is absent`() {
        val json = messageWithMainItems("""[ { "type": "TEXT", "text": "Pay in 4" } ]""")

        assertNull(requireNotNull(PayPalCreditMessagingResult.fromJson(json)).impressionUrl)
    }

    @Test
    fun `fromJson returns a null impressionUrl for a javascript url`() {
        val json = JSONObject(
            """
            {
              "messages": [
                {
                  "preferred_message": {
                    "content": { "main_items": [ { "type": "TEXT", "text": "Pay in 4" } ] },
                    "analytics": { "impression_url": "javascript:alert(1)" }
                  }
                }
              ]
            }
            """.trimIndent()
        )

        val result = requireNotNull(PayPalCreditMessagingResult.fromJson(json))

        assertNull(result.impressionUrl)
    }

    private fun messageWithMainItems(mainItems: String) = JSONObject(
        """{ "messages": [ { "preferred_message": { "content": { "main_items": $mainItems } } } ] }"""
    )
}
