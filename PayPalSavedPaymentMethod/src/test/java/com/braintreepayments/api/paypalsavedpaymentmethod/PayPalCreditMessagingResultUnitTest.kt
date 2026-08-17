package com.braintreepayments.api.paypalsavedpaymentmethod

import com.braintreepayments.api.core.ExperimentalBetaApi
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalBetaApi::class)
class PayPalCreditMessagingResultUnitTest {

    @Test
    fun fromJson_parsesPreferredMessageContentAndLearnMoreAction() {
        val messageJson = JSONObject(
            """
            {"preferred_message":{"id":"msg-1","type":"PLLT_MQ_GZ",
              "content":{
                "main_items":[
                  {"type":"TEXT","text":"As low as ${'$'}10/mo"},
                  {"type":"IMAGE","name":"paypal_logo","source_url":"https://paypal.com/logo.png",
                   "alternative_text":"PayPal"}
                ],
                "action_items":[
                  {"type":"LINK","text":"Learn more","click_url":"https://paypal.com/learn","embeddable":true}
                ]
              },
              "analytics":{"impression_url":"https://paypal.com/impression"}},
             "selection_reasons":[{"code":"DEFAULT_PREFERRED","description":"default"}]}
            """.trimIndent()
        )

        val result = PayPalCreditMessagingResult.fromJson(messageJson)

        assertEquals("msg-1", result.messageId)
        assertEquals("PLLT_MQ_GZ", result.messageType)
        assertEquals(2, result.messageItems.size)

        val textItem = result.messageItems[0] as MessageItem.Text
        assertEquals("As low as \$10/mo", textItem.text)

        val imageItem = result.messageItems[1] as MessageItem.Image
        assertEquals("paypal_logo", imageItem.name)
        assertEquals("https://paypal.com/logo.png", imageItem.sourceUrl)
        assertEquals("PayPal", imageItem.alternativeText)

        assertEquals("Learn more", result.learnMoreText)
        assertEquals("https://paypal.com/learn", result.learnMoreUrl)
        assertEquals("https://paypal.com/impression", result.impressionUrl)
    }

    @Test
    fun fromJson_withMissingOptionalFields_returnsDefaults() {
        val messageJson = JSONObject(
            """{"preferred_message":{"id":"msg-2","type":"PLLT_MQ_GZ"}}"""
        )

        val result = PayPalCreditMessagingResult.fromJson(messageJson)

        assertEquals("msg-2", result.messageId)
        assertTrue(result.messageItems.isEmpty())
        assertNull(result.learnMoreText)
        assertNull(result.learnMoreUrl)
        assertNull(result.impressionUrl)
    }
}
